from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional, Tuple
import math

from .io import GraphData


@dataclass(frozen=True)
class LayoutParams:
    ka: int = 1
    kb: int = 1
    kc: int = 0
    max_rounds: int = 200
    search_steps: int = 100
    eps: float = 1e-8
    x_range: int = 100
    y_range: int = 100
    seed: int = 7


@dataclass(frozen=True)
class LayoutResult:
    positions: List[Tuple[float, float]]
    rounds: int
    final_energy: float


class DeterministicRng:
    _MASK64 = (1 << 64) - 1

    def __init__(self, seed: int) -> None:
        self.state = seed & self._MASK64

    def next_double(self) -> float:
        self.state = (self.state * 6364136223846793005 + 1442695040888963407) & self._MASK64
        bits = (self.state >> 11) & ((1 << 53) - 1)
        return bits * (2.0 ** -53)


class DemaEngine:
    def __init__(self, graph: GraphData, params: LayoutParams) -> None:
        self.graph = graph
        self.params = params
        self.n = graph.node_count

        self.positions = [[0.0, 0.0] for _ in range(self.n)]
        self.weight = [[0.0 for _ in range(self.n)] for _ in range(self.n)]
        self.search = [i / float(self.params.search_steps) for i in range(self.params.search_steps + 1)]
        self.set_membership = self._build_set_membership()

        self._compute_weight()
        self._random_initialize()

    def run(self) -> LayoutResult:
        rounds_completed = 0
        for round_idx in range(self.params.max_rounds):
            if self._any_constraint_violation():
                raise RuntimeError(f"Distance constraint violation before round {round_idx}")

            set_centers = self._compute_set_centers()
            before = self._total_energy(set_centers)

            for i in range(self.n):
                current_energy = self._node_energy(i, set_centers)
                grad = self._normalized_gradient(i, set_centers)
                if grad is None:
                    continue

                step_idx = self._binary_search_step(
                    left=1,
                    right=self.params.search_steps,
                    node=i,
                    dx=grad[0],
                    dy=grad[1],
                    current_energy=current_energy,
                    set_centers=set_centers,
                )
                self.positions[i][0] += grad[0] * self.search[step_idx]
                self.positions[i][1] += grad[1] * self.search[step_idx]

            after = self._total_energy(set_centers)
            rounds_completed = round_idx + 1
            if abs(before - after) <= self.params.eps:
                break

        scaled = self._scale_positions()
        final_energy = self._total_energy(self._compute_set_centers())
        return LayoutResult(positions=scaled, rounds=rounds_completed, final_energy=final_energy)

    def _build_set_membership(self) -> List[List[bool]]:
        membership: List[List[bool]] = []
        for group in self.graph.sets:
            flags = [False] * self.n
            for idx in group:
                flags[idx] = True
            membership.append(flags)
        return membership

    def _compute_weight(self) -> None:
        for i in range(self.n):
            for j in range(self.n):
                if not self.graph.adjacency[i][j]:
                    self.weight[i][j] = 0.0
                    continue

                has_ic = not math.isnan(self.graph.ic[i][j])
                has_ec = not math.isnan(self.graph.ec[i][j])

                if not has_ic and not has_ec:
                    value = 1.0
                elif not has_ic:
                    value = self._transformed_weight(self.graph.ec[i][j])
                elif not has_ec:
                    value = self._transformed_weight(self.graph.ic[i][j])
                else:
                    denom = self._transformed_denominator(self.graph.ic[i][j]) + self._transformed_denominator(self.graph.ec[i][j])
                    if abs(denom) < self.params.eps:
                        denom = self.params.eps
                    value = (1.0 / denom) + 1.0

                if value < self.params.eps:
                    value = self.params.eps
                self.weight[i][j] = value

    def _transformed_weight(self, v: float) -> float:
        denom = self._transformed_denominator(v)
        if abs(denom) < self.params.eps:
            denom = self.params.eps
        return (1.0 / denom) + 1.0

    @staticmethod
    def _transformed_denominator(v: float) -> float:
        clamped = max(-0.999999999, min(0.999999999, v))
        return -math.log((1.0 - clamped) / (1.0 + clamped))

    def _random_initialize(self) -> None:
        rng = DeterministicRng(self.params.seed)

        for i in range(self.n):
            placed = False
            attempts = 0

            while not placed and attempts < 20000:
                attempts += 1

                x = rng.next_double()
                y = rng.next_double()
                d = self._norm(x, y)
                if d <= self.params.eps:
                    continue

                x /= d * 4.0
                y /= d * 4.0
                self.positions[i][0] = x
                self.positions[i][1] = y

                placed = True
                for j in range(i):
                    dis = self._distance(self.positions[i], self.positions[j])
                    if dis <= self.params.eps:
                        placed = False
                        break
                    if self.graph.adjacency[i][j] and dis >= self.weight[i][j]:
                        placed = False
                        break

            if not placed:
                self.positions[i][0] = (i + 1) / float(self.n + 1) / 4.0
                self.positions[i][1] = (self.n - i) / float(self.n + 1) / 4.0

    def _compute_set_centers(self) -> List[List[float]]:
        centers = [[0.0, 0.0] for _ in self.graph.sets]
        for s, group in enumerate(self.graph.sets):
            if not group:
                continue
            x = 0.0
            y = 0.0
            for idx in group:
                x += self.positions[idx][0]
                y += self.positions[idx][1]
            centers[s][0] = x / len(group)
            centers[s][1] = y / len(group)
        return centers

    def _any_constraint_violation(self) -> bool:
        return any(self._violates_node_constraint(i) for i in range(self.n))

    def _violates_node_constraint(self, node: int) -> bool:
        for j in range(self.n):
            if self.graph.adjacency[node][j]:
                if self._distance(self.positions[node], self.positions[j]) > self.weight[node][j]:
                    return True
        return False

    def _node_energy(self, node: int, set_centers: List[List[float]]) -> float:
        e = 0.0
        for i in range(self.n):
            if i == node:
                continue
            dis = max(self._distance(self.positions[node], self.positions[i]), self.params.eps)
            e += self.params.ka * self.graph.fold_changes[node] * self.graph.fold_changes[i] / dis

            if self.graph.adjacency[node][i]:
                denom = max(self.weight[node][i] - dis, self.params.eps)
                e += self.params.kb * self.graph.fold_changes[node] * self.graph.fold_changes[i] / denom

        for s in range(len(self.graph.sets)):
            if self.set_membership[s][node]:
                dis = self._distance(self.positions[node], set_centers[s])
                e += dis * dis * self.params.kc

        return e

    def _total_energy(self, set_centers: List[List[float]]) -> float:
        total = 0.0
        for i in range(self.n):
            total += self._node_energy(i, set_centers)
        return total / 2.0

    def _normalized_gradient(self, node: int, set_centers: List[List[float]]) -> Optional[Tuple[float, float]]:
        gx = 0.0
        gy = 0.0

        for j in range(self.n):
            if j == node:
                continue
            dis = max(self._distance(self.positions[node], self.positions[j]), self.params.eps)
            dx = self.positions[j][0] - self.positions[node][0]
            dy = self.positions[j][1] - self.positions[node][1]

            repel = self.params.ka * self.graph.fold_changes[node] * self.graph.fold_changes[j] / (dis * dis)
            gx -= (dx / dis) * repel
            gy -= (dy / dis) * repel

            if self.graph.adjacency[node][j]:
                denom = max(self.weight[node][j] - dis, self.params.eps)
                attract = self.params.kb * self.graph.fold_changes[node] * self.graph.fold_changes[j] / (denom * denom)
                gx += (dx / dis) * attract
                gy += (dy / dis) * attract

        for s in range(len(self.graph.sets)):
            if self.set_membership[s][node]:
                dis = self._distance(self.positions[node], set_centers[s])
                gx += (set_centers[s][0] - self.positions[node][0]) * 2.0 * dis * self.params.kc
                gy += (set_centers[s][1] - self.positions[node][1]) * 2.0 * dis * self.params.kc

        norm = self._norm(gx, gy)
        if norm <= self.params.eps or math.isnan(norm):
            return None
        return gx / norm, gy / norm

    def _binary_search_step(
        self,
        left: int,
        right: int,
        node: int,
        dx: float,
        dy: float,
        current_energy: float,
        set_centers: List[List[float]],
    ) -> int:
        if left > right:
            return 0

        mid = (left + right) // 2
        self.positions[node][0] += dx * self.search[mid]
        self.positions[node][1] += dy * self.search[mid]

        improves = self._node_energy(node, set_centers) < current_energy and not self._violates_node_constraint(node)

        self.positions[node][0] -= dx * self.search[mid]
        self.positions[node][1] -= dy * self.search[mid]

        if improves:
            t = self._binary_search_step(mid + 1, right, node, dx, dy, current_energy, set_centers)
            return mid if t == 0 else t
        return self._binary_search_step(left, mid - 1, node, dx, dy, current_energy, set_centers)

    def _scale_positions(self) -> List[Tuple[float, float]]:
        if self.n == 0:
            return []

        x_min = self.positions[0][0]
        x_max = self.positions[0][0]
        y_min = self.positions[0][1]
        y_max = self.positions[0][1]

        for i in range(1, self.n):
            x_min = min(x_min, self.positions[i][0])
            x_max = max(x_max, self.positions[i][0])
            y_min = min(y_min, self.positions[i][1])
            y_max = max(y_max, self.positions[i][1])

        x_mid = (x_min + x_max) / 2.0
        y_mid = (y_min + y_max) / 2.0
        max_len = max(x_max - x_min, y_max - y_min)
        if max_len <= self.params.eps:
            max_len = 1.0

        expand_factor = 0.1
        scaled: List[Tuple[float, float]] = []
        for i in range(self.n):
            x = self.params.x_range * (self.positions[i][0] - x_mid) / max_len / expand_factor
            y = self.params.y_range * (self.positions[i][1] - y_mid) / max_len / expand_factor
            scaled.append((x, y))
        return scaled

    @staticmethod
    def _distance(a: List[float], b: List[float]) -> float:
        return DemaEngine._norm(a[0] - b[0], a[1] - b[1])

    @staticmethod
    def _norm(x: float, y: float) -> float:
        return math.sqrt(x * x + y * y)
