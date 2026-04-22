from __future__ import annotations

from concurrent.futures import ProcessPoolExecutor, ThreadPoolExecutor
from dataclasses import dataclass
from typing import List, Optional, Sequence, Tuple
import math
import os

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
    workers: int = max(1, (os.cpu_count() or 2) - 1)


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


def _distance(a: Sequence[float], b: Sequence[float]) -> float:
    x = a[0] - b[0]
    y = a[1] - b[1]
    return math.sqrt(x * x + y * y)


def _norm(x: float, y: float) -> float:
    return math.sqrt(x * x + y * y)


def _node_energy_snapshot(
    node: int,
    candidate: Optional[Tuple[float, float]],
    positions: Sequence[Sequence[float]],
    set_centers: Sequence[Sequence[float]],
    adjacency: Sequence[Sequence[bool]],
    weight: Sequence[Sequence[float]],
    fc: Sequence[float],
    set_membership: Sequence[Sequence[bool]],
    ka: int,
    kb: int,
    kc: int,
    eps: float,
) -> float:
    px, py = (candidate if candidate is not None else (positions[node][0], positions[node][1]))
    e = 0.0

    for i in range(len(positions)):
        if i == node:
            continue
        ox, oy = positions[i][0], positions[i][1]
        dis = max(math.sqrt((px - ox) * (px - ox) + (py - oy) * (py - oy)), eps)
        e += ka * fc[node] * fc[i] / dis
        if adjacency[node][i]:
            denom = max(weight[node][i] - dis, eps)
            e += kb * fc[node] * fc[i] / denom

    for s in range(len(set_membership)):
        if set_membership[s][node]:
            dx = px - set_centers[s][0]
            dy = py - set_centers[s][1]
            dis = math.sqrt(dx * dx + dy * dy)
            e += dis * dis * kc

    return e


def _violates_constraint_snapshot(
    node: int,
    candidate: Tuple[float, float],
    positions: Sequence[Sequence[float]],
    adjacency: Sequence[Sequence[bool]],
    weight: Sequence[Sequence[float]],
) -> bool:
    for j in range(len(positions)):
        if adjacency[node][j]:
            dis = math.sqrt(
                (candidate[0] - positions[j][0]) * (candidate[0] - positions[j][0])
                + (candidate[1] - positions[j][1]) * (candidate[1] - positions[j][1])
            )
            if dis > weight[node][j]:
                return True
    return False


def _displacement_for_node(args: Tuple) -> Tuple[int, float, float]:
    (
        node,
        positions,
        set_centers,
        adjacency,
        weight,
        fc,
        set_membership,
        ka,
        kb,
        kc,
        eps,
        search,
    ) = args

    n = len(positions)
    px, py = positions[node][0], positions[node][1]
    max_step = float("inf")
    for j in range(n):
        if adjacency[node][j] and j != node:
            d = math.sqrt(
                (px - positions[j][0]) * (px - positions[j][0])
                + (py - positions[j][1]) * (py - positions[j][1])
            )
            slack = weight[node][j] - d
            max_step = min(max_step, (slack / 2.0) - 1e-9)
    if math.isinf(max_step):
        max_step = search[-1]
    if max_step < 0:
        max_step = 0.0

    current_energy = _node_energy_snapshot(
        node,
        None,
        positions,
        set_centers,
        adjacency,
        weight,
        fc,
        set_membership,
        ka,
        kb,
        kc,
        eps,
    )

    gx = 0.0
    gy = 0.0
    for j in range(n):
        if j == node:
            continue
        dx = positions[j][0] - px
        dy = positions[j][1] - py
        dis = max(math.sqrt(dx * dx + dy * dy), eps)

        repel = ka * fc[node] * fc[j] / (dis * dis)
        gx -= (dx / dis) * repel
        gy -= (dy / dis) * repel

        if adjacency[node][j]:
            denom = max(weight[node][j] - dis, eps)
            attract = kb * fc[node] * fc[j] / (denom * denom)
            gx += (dx / dis) * attract
            gy += (dy / dis) * attract

    for s in range(len(set_membership)):
        if set_membership[s][node]:
            dxs = set_centers[s][0] - px
            dys = set_centers[s][1] - py
            dis = math.sqrt(dxs * dxs + dys * dys)
            gx += dxs * 2.0 * dis * kc
            gy += dys * 2.0 * dis * kc

    gnorm = _norm(gx, gy)
    if gnorm <= eps or math.isnan(gnorm):
        return node, 0.0, 0.0

    ux = gx / gnorm
    uy = gy / gnorm

    def binary_search(left: int, right: int) -> int:
        if left > right:
            return 0
        mid = (left + right) // 2
        cand = (px + ux * search[mid], py + uy * search[mid])
        improves = (
            _node_energy_snapshot(
                node,
                cand,
                positions,
                set_centers,
                adjacency,
                weight,
                fc,
                set_membership,
                ka,
                kb,
                kc,
                eps,
            )
            < current_energy
            and search[mid] < max_step
            and not _violates_constraint_snapshot(node, cand, positions, adjacency, weight)
        )
        if improves:
            t = binary_search(mid + 1, right)
            return mid if t == 0 else t
        return binary_search(left, mid - 1)

    step_idx = binary_search(1, len(search) - 1)
    return node, ux * search[step_idx], uy * search[step_idx]


class ParallelDemaEngine:
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
        pool = self._make_pool()
        with pool:
            for round_idx in range(self.params.max_rounds):
                if self._any_constraint_violation():
                    raise RuntimeError(f"Distance constraint violation before round {round_idx}")

                set_centers = self._compute_set_centers()
                before = self._total_energy(set_centers)

                snapshot = tuple((p[0], p[1]) for p in self.positions)
                task_args = [
                    (
                        i,
                        snapshot,
                        tuple((c[0], c[1]) for c in set_centers),
                        self.graph.adjacency,
                        self.weight,
                        self.graph.fold_changes,
                        self.set_membership,
                        self.params.ka,
                        self.params.kb,
                        self.params.kc,
                        self.params.eps,
                        self.search,
                    )
                    for i in range(self.n)
                ]

                updates = list(pool.map(_displacement_for_node, task_args))
                updates.sort(key=lambda x: x[0])

                for node, dx, dy in updates:
                    self.positions[node][0] += dx
                    self.positions[node][1] += dy

                after = self._total_energy(set_centers)
                rounds_completed = round_idx + 1
                if abs(before - after) <= self.params.eps:
                    break

        scaled = self._scale_positions()
        final_energy = self._total_energy(self._compute_set_centers())
        return LayoutResult(positions=scaled, rounds=rounds_completed, final_energy=final_energy)

    def _make_pool(self):
        workers = max(1, self.params.workers)
        try:
            return ProcessPoolExecutor(max_workers=workers)
        except (OSError, PermissionError):
            # Some restricted environments disallow process semaphore limits.
            return ThreadPoolExecutor(max_workers=workers)

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
                d = _norm(x, y)
                if d <= self.params.eps:
                    continue
                x /= d * 4.0
                y /= d * 4.0
                self.positions[i][0] = x
                self.positions[i][1] = y

                placed = True
                for j in range(i):
                    dis = _distance(self.positions[i], self.positions[j])
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

    def _violates_node_constraint(self, node: int) -> bool:
        for j in range(self.n):
            if self.graph.adjacency[node][j]:
                if _distance(self.positions[node], self.positions[j]) > self.weight[node][j]:
                    return True
        return False

    def _any_constraint_violation(self) -> bool:
        return any(self._violates_node_constraint(i) for i in range(self.n))

    def _node_energy(self, node: int, set_centers: List[List[float]]) -> float:
        return _node_energy_snapshot(
            node,
            None,
            self.positions,
            set_centers,
            self.graph.adjacency,
            self.weight,
            self.graph.fold_changes,
            self.set_membership,
            self.params.ka,
            self.params.kb,
            self.params.kc,
            self.params.eps,
        )

    def _total_energy(self, set_centers: List[List[float]]) -> float:
        total = 0.0
        for i in range(self.n):
            total += self._node_energy(i, set_centers)
        return total / 2.0

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
