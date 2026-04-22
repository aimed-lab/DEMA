from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional, Tuple
import math

import numpy as np

from .io import GraphData

try:
    from numba import njit
    HAS_NUMBA = True
except Exception:  # pragma: no cover
    HAS_NUMBA = False

    def njit(*_args, **_kwargs):  # type: ignore
        def deco(fn):
            return fn
        return deco


@njit(cache=True)
def _compute_set_centers_nb(positions, set_membership):
    s_count, n = set_membership.shape
    centers = np.zeros((s_count, 2), dtype=np.float64)
    for s in range(s_count):
        sx = 0.0
        sy = 0.0
        c = 0
        for i in range(n):
            if set_membership[s, i]:
                sx += positions[i, 0]
                sy += positions[i, 1]
                c += 1
        if c > 0:
            centers[s, 0] = sx / c
            centers[s, 1] = sy / c
    return centers


@njit(cache=True)
def _node_energy_nb(node, px, py, positions, adjacency, weight, fc, set_membership, set_centers, ka, kb, kc, eps):
    n = positions.shape[0]
    e = 0.0

    for i in range(n):
        if i == node:
            continue
        dx = px - positions[i, 0]
        dy = py - positions[i, 1]
        dis = math.sqrt(dx * dx + dy * dy)
        if dis < eps:
            dis = eps

        e += ka * fc[node] * fc[i] / dis
        if adjacency[node, i]:
            denom = weight[node, i] - dis
            if denom < eps:
                denom = eps
            e += kb * fc[node] * fc[i] / denom

    s_count = set_membership.shape[0]
    for s in range(s_count):
        if set_membership[s, node]:
            dx = px - set_centers[s, 0]
            dy = py - set_centers[s, 1]
            dis = math.sqrt(dx * dx + dy * dy)
            e += dis * dis * kc

    return e


@njit(cache=True)
def _gradient_nb(node, positions, adjacency, weight, fc, set_membership, set_centers, ka, kb, kc, eps):
    n = positions.shape[0]
    px = positions[node, 0]
    py = positions[node, 1]

    gx = 0.0
    gy = 0.0

    for j in range(n):
        if j == node:
            continue

        dx = positions[j, 0] - px
        dy = positions[j, 1] - py
        dis = math.sqrt(dx * dx + dy * dy)
        if dis < eps:
            dis = eps

        repel = ka * fc[node] * fc[j] / (dis * dis)
        gx -= (dx / dis) * repel
        gy -= (dy / dis) * repel

        if adjacency[node, j]:
            denom = weight[node, j] - dis
            if denom < eps:
                denom = eps
            attract = kb * fc[node] * fc[j] / (denom * denom)
            gx += (dx / dis) * attract
            gy += (dy / dis) * attract

    s_count = set_membership.shape[0]
    for s in range(s_count):
        if set_membership[s, node]:
            dx = set_centers[s, 0] - px
            dy = set_centers[s, 1] - py
            dis = math.sqrt(dx * dx + dy * dy)
            gx += dx * 2.0 * dis * kc
            gy += dy * 2.0 * dis * kc

    norm = math.sqrt(gx * gx + gy * gy)
    if norm <= eps or math.isnan(norm):
        return 0.0, 0.0, False

    return gx / norm, gy / norm, True


@njit(cache=True)
def _violates_candidate_nb(node, cx, cy, positions, adjacency, weight):
    n = positions.shape[0]
    for j in range(n):
        if adjacency[node, j]:
            dx = cx - positions[j, 0]
            dy = cy - positions[j, 1]
            dis = math.sqrt(dx * dx + dy * dy)
            if dis > weight[node, j]:
                return True
    return False


@njit(cache=True)
def _any_constraint_violation_nb(positions, adjacency, weight):
    n = positions.shape[0]
    for i in range(n):
        for j in range(n):
            if adjacency[i, j]:
                dx = positions[i, 0] - positions[j, 0]
                dy = positions[i, 1] - positions[j, 1]
                dis = math.sqrt(dx * dx + dy * dy)
                if dis > weight[i, j]:
                    return True
    return False


@njit(cache=True)
def _total_energy_nb(positions, adjacency, weight, fc, set_membership, set_centers, ka, kb, kc, eps):
    n = positions.shape[0]
    total = 0.0
    for node in range(n):
        total += _node_energy_nb(
            node,
            positions[node, 0],
            positions[node, 1],
            positions,
            adjacency,
            weight,
            fc,
            set_membership,
            set_centers,
            ka,
            kb,
            kc,
            eps,
        )
    return total / 2.0


@njit(cache=True)
def _binary_search_step_nb(node, positions, adjacency, weight, fc, set_membership, set_centers, ka, kb, kc, eps, search, dx, dy, current_energy):
    left = 1
    right = search.shape[0] - 1
    best = 0

    base_x = positions[node, 0]
    base_y = positions[node, 1]

    while left <= right:
        mid = (left + right) // 2
        cx = base_x + dx * search[mid]
        cy = base_y + dy * search[mid]

        cand_energy = _node_energy_nb(node, cx, cy, positions, adjacency, weight, fc, set_membership, set_centers, ka, kb, kc, eps)
        improves = cand_energy < current_energy and not _violates_candidate_nb(node, cx, cy, positions, adjacency, weight)

        if improves:
            best = mid
            left = mid + 1
        else:
            right = mid - 1

    return best


@njit(cache=True)
def _run_numba(positions, adjacency, weight, fc, set_membership, ka, kb, kc, max_rounds, eps, search):
    rounds_completed = 0

    for round_idx in range(max_rounds):
        if _any_constraint_violation_nb(positions, adjacency, weight):
            return -1, 0.0

        set_centers = _compute_set_centers_nb(positions, set_membership)
        before = _total_energy_nb(positions, adjacency, weight, fc, set_membership, set_centers, ka, kb, kc, eps)

        n = positions.shape[0]
        for node in range(n):
            current_energy = _node_energy_nb(
                node,
                positions[node, 0],
                positions[node, 1],
                positions,
                adjacency,
                weight,
                fc,
                set_membership,
                set_centers,
                ka,
                kb,
                kc,
                eps,
            )

            gx, gy, ok = _gradient_nb(node, positions, adjacency, weight, fc, set_membership, set_centers, ka, kb, kc, eps)
            if ok:
                step_idx = _binary_search_step_nb(
                    node,
                    positions,
                    adjacency,
                    weight,
                    fc,
                    set_membership,
                    set_centers,
                    ka,
                    kb,
                    kc,
                    eps,
                    search,
                    gx,
                    gy,
                    current_energy,
                )
                step = search[step_idx]
                positions[node, 0] += gx * step
                positions[node, 1] += gy * step

        after = _total_energy_nb(positions, adjacency, weight, fc, set_membership, set_centers, ka, kb, kc, eps)
        rounds_completed = round_idx + 1
        if abs(before - after) <= eps:
            break

    final_centers = _compute_set_centers_nb(positions, set_membership)
    final_energy = _total_energy_nb(positions, adjacency, weight, fc, set_membership, final_centers, ka, kb, kc, eps)
    return rounds_completed, final_energy


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
    _jit_warmed = False

    def __init__(self, graph: GraphData, params: LayoutParams) -> None:
        self.graph = graph
        self.params = params
        self.n = graph.node_count

        self.positions = np.zeros((self.n, 2), dtype=np.float64)
        self.weight = np.zeros((self.n, self.n), dtype=np.float64)
        self.search = np.linspace(0.0, 1.0, self.params.search_steps + 1, dtype=np.float64)

        self.adjacency = np.asarray(graph.adjacency, dtype=np.bool_)
        self.ic = np.asarray(graph.ic, dtype=np.float64)
        self.ec = np.asarray(graph.ec, dtype=np.float64)
        self.fc = np.asarray(graph.fold_changes, dtype=np.float64)

        self.set_indices: List[np.ndarray] = [np.asarray(group, dtype=np.int64) for group in graph.sets]
        self.set_membership = self._build_set_membership()

        self._compute_weight()
        self._random_initialize()

    @classmethod
    def warmup_jit(cls) -> None:
        if not HAS_NUMBA or cls._jit_warmed:
            return

        positions = np.zeros((2, 2), dtype=np.float64)
        adjacency = np.array([[False, True], [True, False]], dtype=np.bool_)
        weight = np.array([[0.0, 1.0], [1.0, 0.0]], dtype=np.float64)
        fc = np.array([1.0, 1.0], dtype=np.float64)
        set_membership = np.zeros((1, 2), dtype=np.bool_)
        search = np.linspace(0.0, 1.0, 8, dtype=np.float64)

        _run_numba(positions, adjacency, weight, fc, set_membership, 1, 1, 0, 1, 1e-8, search)
        cls._jit_warmed = True

    def run(self) -> LayoutResult:
        if HAS_NUMBA:
            rounds_completed, final_energy = _run_numba(
                self.positions,
                self.adjacency,
                self.weight,
                self.fc,
                self.set_membership,
                self.params.ka,
                self.params.kb,
                self.params.kc,
                self.params.max_rounds,
                self.params.eps,
                self.search,
            )
            if rounds_completed < 0:
                raise RuntimeError("Distance constraint violation before round 0")
        else:
            rounds_completed, final_energy = self._run_numpy_fallback()

        scaled = self._scale_positions()
        return LayoutResult(positions=[(float(x), float(y)) for x, y in scaled], rounds=int(rounds_completed), final_energy=float(final_energy))

    def _run_numpy_fallback(self) -> Tuple[int, float]:
        rounds_completed = 0
        for round_idx in range(self.params.max_rounds):
            if self._any_constraint_violation_numpy():
                raise RuntimeError(f"Distance constraint violation before round {round_idx}")

            set_centers = self._compute_set_centers_numpy()
            before = self._total_energy_numpy(set_centers)

            for node in range(self.n):
                current_energy = self._node_energy_numpy(node, set_centers)
                grad = self._normalized_gradient_numpy(node, set_centers)
                if grad is None:
                    continue

                step_idx = self._binary_search_step_numpy(node, grad[0], grad[1], current_energy, set_centers)
                step = self.search[step_idx]
                self.positions[node, 0] += grad[0] * step
                self.positions[node, 1] += grad[1] * step

            after = self._total_energy_numpy(set_centers)
            rounds_completed = round_idx + 1
            if abs(before - after) <= self.params.eps:
                break

        final_energy = self._total_energy_numpy(self._compute_set_centers_numpy())
        return rounds_completed, final_energy

    def _build_set_membership(self) -> np.ndarray:
        membership = np.zeros((len(self.set_indices), self.n), dtype=np.bool_)
        for s, members in enumerate(self.set_indices):
            if members.size:
                membership[s, members] = True
        return membership

    def _compute_weight(self) -> None:
        adj = self.adjacency
        has_ic = ~np.isnan(self.ic)
        has_ec = ~np.isnan(self.ec)

        self.weight.fill(0.0)

        no_meta = adj & ~has_ic & ~has_ec
        self.weight[no_meta] = 1.0

        only_ic = adj & has_ic & ~has_ec
        if np.any(only_ic):
            self.weight[only_ic] = self._transformed_weight_array(self.ic[only_ic])

        only_ec = adj & ~has_ic & has_ec
        if np.any(only_ec):
            self.weight[only_ec] = self._transformed_weight_array(self.ec[only_ec])

        both = adj & has_ic & has_ec
        if np.any(both):
            denom = self._transformed_denominator_array(self.ic[both]) + self._transformed_denominator_array(self.ec[both])
            denom = np.where(np.abs(denom) < self.params.eps, self.params.eps, denom)
            self.weight[both] = (1.0 / denom) + 1.0

        self.weight[adj] = np.maximum(self.weight[adj], self.params.eps)

    def _transformed_weight_array(self, values: np.ndarray) -> np.ndarray:
        denom = self._transformed_denominator_array(values)
        denom = np.where(np.abs(denom) < self.params.eps, self.params.eps, denom)
        return (1.0 / denom) + 1.0

    @staticmethod
    def _transformed_denominator_array(values: np.ndarray) -> np.ndarray:
        clamped = np.clip(values, -0.999999999, 0.999999999)
        return -np.log((1.0 - clamped) / (1.0 + clamped))

    def _random_initialize(self) -> None:
        rng = DeterministicRng(self.params.seed)

        for i in range(self.n):
            placed = False
            attempts = 0

            while not placed and attempts < 20000:
                attempts += 1

                x = rng.next_double()
                y = rng.next_double()
                d = math.hypot(x, y)
                if d <= self.params.eps:
                    continue

                x /= d * 4.0
                y /= d * 4.0
                self.positions[i, 0] = x
                self.positions[i, 1] = y

                placed = True
                if i > 0:
                    prev = self.positions[:i]
                    dx = prev[:, 0] - x
                    dy = prev[:, 1] - y
                    dis = np.hypot(dx, dy)

                    if np.any(dis <= self.params.eps):
                        placed = False
                        continue

                    connected = self.adjacency[i, :i]
                    if np.any(connected & (dis >= self.weight[i, :i])):
                        placed = False

            if not placed:
                self.positions[i, 0] = (i + 1) / float(self.n + 1) / 4.0
                self.positions[i, 1] = (self.n - i) / float(self.n + 1) / 4.0

    def _compute_set_centers_numpy(self) -> np.ndarray:
        centers = np.zeros((len(self.set_indices), 2), dtype=np.float64)
        for s, members in enumerate(self.set_indices):
            if members.size:
                centers[s] = np.mean(self.positions[members], axis=0)
        return centers

    def _any_constraint_violation_numpy(self) -> bool:
        if self.n == 0:
            return False
        delta = self.positions[:, None, :] - self.positions[None, :, :]
        dist = np.hypot(delta[:, :, 0], delta[:, :, 1])
        return bool(np.any(self.adjacency & (dist > self.weight)))

    def _node_energy_numpy(self, node: int, set_centers: np.ndarray, candidate: Optional[np.ndarray] = None) -> float:
        pos = self.positions[node] if candidate is None else candidate

        delta = self.positions - pos
        dist = np.hypot(delta[:, 0], delta[:, 1])
        dist[node] = np.inf
        dist = np.maximum(dist, self.params.eps)

        fc_node = self.fc[node]
        e = self.params.ka * fc_node * float(np.sum(self.fc / dist))

        adj_row = self.adjacency[node]
        if np.any(adj_row):
            denom = np.maximum(self.weight[node, adj_row] - dist[adj_row], self.params.eps)
            e += self.params.kb * fc_node * float(np.sum(self.fc[adj_row] / denom))

        if self.set_membership.shape[0]:
            member_sets = np.nonzero(self.set_membership[:, node])[0]
            for s in member_sets:
                d = math.hypot(pos[0] - set_centers[s, 0], pos[1] - set_centers[s, 1])
                e += d * d * self.params.kc

        return e

    def _total_energy_numpy(self, set_centers: np.ndarray) -> float:
        total = 0.0
        for node in range(self.n):
            total += self._node_energy_numpy(node, set_centers)
        return total / 2.0

    def _normalized_gradient_numpy(self, node: int, set_centers: np.ndarray) -> Optional[Tuple[float, float]]:
        pos = self.positions[node]
        delta = self.positions - pos
        dist = np.hypot(delta[:, 0], delta[:, 1])
        dist[node] = np.inf
        dist_safe = np.maximum(dist, self.params.eps)

        fc_node = self.fc[node]
        repel = self.params.ka * fc_node * self.fc / (dist_safe * dist_safe)
        unit = delta / dist_safe[:, None]

        gx = -float(np.sum(unit[:, 0] * repel))
        gy = -float(np.sum(unit[:, 1] * repel))

        adj_row = self.adjacency[node]
        if np.any(adj_row):
            denom = np.maximum(self.weight[node, adj_row] - dist_safe[adj_row], self.params.eps)
            attract = self.params.kb * fc_node * self.fc[adj_row] / (denom * denom)
            gx += float(np.sum(unit[adj_row, 0] * attract))
            gy += float(np.sum(unit[adj_row, 1] * attract))

        if self.set_membership.shape[0]:
            member_sets = np.nonzero(self.set_membership[:, node])[0]
            for s in member_sets:
                dx = set_centers[s, 0] - pos[0]
                dy = set_centers[s, 1] - pos[1]
                d = math.hypot(dx, dy)
                gx += dx * 2.0 * d * self.params.kc
                gy += dy * 2.0 * d * self.params.kc

        norm = math.hypot(gx, gy)
        if norm <= self.params.eps or math.isnan(norm):
            return None
        return gx / norm, gy / norm

    def _violates_candidate_numpy(self, node: int, candidate: np.ndarray) -> bool:
        adj_row = self.adjacency[node]
        if not np.any(adj_row):
            return False
        others = self.positions[adj_row]
        if others.size == 0:
            return False
        d = np.hypot(others[:, 0] - candidate[0], others[:, 1] - candidate[1])
        w = self.weight[node, adj_row]
        return bool(np.any(d > w))

    def _binary_search_step_numpy(self, node: int, dx: float, dy: float, current_energy: float, set_centers: np.ndarray) -> int:
        left = 1
        right = self.params.search_steps
        best = 0

        base = self.positions[node].copy()

        while left <= right:
            mid = (left + right) // 2
            step = self.search[mid]
            candidate = np.array([base[0] + dx * step, base[1] + dy * step], dtype=np.float64)

            improves = self._node_energy_numpy(node, set_centers, candidate) < current_energy and not self._violates_candidate_numpy(node, candidate)

            if improves:
                best = mid
                left = mid + 1
            else:
                right = mid - 1

        return best

    def _scale_positions(self) -> np.ndarray:
        if self.n == 0:
            return np.zeros((0, 2), dtype=np.float64)

        x_min = float(np.min(self.positions[:, 0]))
        x_max = float(np.max(self.positions[:, 0]))
        y_min = float(np.min(self.positions[:, 1]))
        y_max = float(np.max(self.positions[:, 1]))

        x_mid = (x_min + x_max) / 2.0
        y_mid = (y_min + y_max) / 2.0
        max_len = max(x_max - x_min, y_max - y_min)
        if max_len <= self.params.eps:
            max_len = 1.0

        expand_factor = 0.1
        scaled = np.zeros_like(self.positions)
        scaled[:, 0] = self.params.x_range * (self.positions[:, 0] - x_mid) / max_len / expand_factor
        scaled[:, 1] = self.params.y_range * (self.positions[:, 1] - y_mid) / max_len / expand_factor
        return scaled
