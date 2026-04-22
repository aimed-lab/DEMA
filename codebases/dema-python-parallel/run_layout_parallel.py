#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path
import time

from dema_py_parallel.engine import HAS_NUMBA, LayoutParams, ParallelDemaEngine
from dema_py_parallel.io import load_tsv_graph


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Run process-parallel DEMA layout")
    parser.add_argument("--nodes", required=True)
    parser.add_argument("--edges", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--ka", type=int, default=1)
    parser.add_argument("--kb", type=int, default=-1)
    parser.add_argument("--kc", type=int, default=-1)
    parser.add_argument("--rounds", type=int, default=200)
    parser.add_argument("--search-steps", type=int, default=100)
    parser.add_argument("--eps", type=float, default=1e-8)
    parser.add_argument("--xrange", type=int, default=100)
    parser.add_argument("--yrange", type=int, default=100)
    parser.add_argument("--seed", type=int, default=7)
    return parser


def main() -> None:
    args = build_parser().parse_args()
    graph = load_tsv_graph(Path(args.nodes), Path(args.edges))

    kb = graph.node_count if args.kb < 0 else args.kb
    kc = (graph.node_count * 10 if graph.sets else 0) if args.kc < 0 else args.kc

    params = LayoutParams(
        ka=args.ka,
        kb=kb,
        kc=kc,
        max_rounds=args.rounds,
        search_steps=args.search_steps,
        eps=args.eps,
        x_range=args.xrange,
        y_range=args.yrange,
        seed=args.seed,
        workers=max(1, args.workers),
    )

    ParallelDemaEngine.warmup_jit()
    started = time.perf_counter()
    engine = ParallelDemaEngine(graph, params)
    result = engine.run()
    elapsed_ms = (time.perf_counter() - started) * 1000.0

    output_path = Path(args.output)
    with output_path.open("w", encoding="utf-8") as f:
        f.write("id\tx\ty\n")
        for node_id, (x, y) in zip(graph.node_ids, result.positions):
            f.write(f"{node_id}\t{x:.12f}\t{y:.12f}\n")

    print(
        f"DEMA-PY-PAR rounds={result.rounds} "
        f"energy={result.final_energy:.12f} elapsed_ms={elapsed_ms:.3f} "
        f"workers={params.workers} effective_workers={engine.effective_workers_used} "
        f"backend={'numba-parallel' if HAS_NUMBA else 'numpy-fallback'} "
        f"output={output_path.resolve()}"
    )


if __name__ == "__main__":
    main()
