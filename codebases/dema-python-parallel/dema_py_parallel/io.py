from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List
import math


@dataclass(frozen=True)
class GraphData:
    node_ids: List[str]
    adjacency: List[List[bool]]
    fold_changes: List[float]
    ic: List[List[float]]
    ec: List[List[float]]
    sets: List[List[int]]

    @property
    def node_count(self) -> int:
        return len(self.node_ids)


def load_tsv_graph(nodes_path: Path, edges_path: Path) -> GraphData:
    node_lines = nodes_path.read_text(encoding="utf-8").splitlines()
    edge_lines = edges_path.read_text(encoding="utf-8").splitlines()

    if len(node_lines) < 2:
        raise ValueError("nodes.tsv must have a header and at least one row")
    if len(edge_lines) < 2:
        raise ValueError("edges.tsv must have a header and at least one row")

    node_ids: List[str] = []
    fold_changes: List[float] = []
    id_to_index: Dict[str, int] = {}
    set_to_nodes: Dict[str, List[int]] = {}

    for row in node_lines[1:]:
        row = row.strip()
        if not row:
            continue
        parts = row.split("\t")
        node_id = parts[0].strip()
        if not node_id:
            continue
        if node_id in id_to_index:
            raise ValueError(f"Duplicate node id: {node_id}")

        idx = len(node_ids)
        id_to_index[node_id] = idx
        node_ids.append(node_id)

        fc = 1.0
        if len(parts) > 1 and parts[1].strip():
            fc = float(parts[1].strip())
        fold_changes.append(fc)

        if len(parts) > 2 and parts[2].strip():
            for token in parts[2].split(","):
                name = token.strip()
                if not name:
                    continue
                set_to_nodes.setdefault(name, []).append(idx)

    n = len(node_ids)
    adjacency = [[False for _ in range(n)] for _ in range(n)]
    ic = [[math.nan for _ in range(n)] for _ in range(n)]
    ec = [[math.nan for _ in range(n)] for _ in range(n)]

    for row in edge_lines[1:]:
        row = row.strip()
        if not row:
            continue
        parts = row.split("\t")
        if len(parts) < 2:
            continue
        src = parts[0].strip()
        dst = parts[1].strip()
        if src not in id_to_index or dst not in id_to_index:
            raise ValueError(f"Unknown node in edge {src} -> {dst}")
        a = id_to_index[src]
        b = id_to_index[dst]
        adjacency[a][b] = True
        adjacency[b][a] = True

        if len(parts) > 2 and parts[2].strip():
            v = float(parts[2].strip())
            ic[a][b] = v
            ic[b][a] = v

        if len(parts) > 3 and parts[3].strip():
            v = float(parts[3].strip())
            ec[a][b] = v
            ec[b][a] = v

    sets = [members for _, members in set_to_nodes.items()]
    return GraphData(
        node_ids=node_ids,
        adjacency=adjacency,
        fold_changes=fold_changes,
        ic=ic,
        ec=ec,
        sets=sets,
    )
