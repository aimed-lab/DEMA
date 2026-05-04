#!/usr/bin/env python3
from __future__ import annotations

import datetime as dt
import re
import statistics
import subprocess
from pathlib import Path
from typing import List, Tuple

ROOT = Path(__file__).resolve().parents[1]
TMP = ROOT / ".bench_tmp"
REPORT = ROOT / "benchmarks" / "scaling-report.md"

PY_BASE = ROOT / "codebases" / "dema-python"
PYP_BASE = ROOT / "codebases" / "dema-python-parallel"

NODES_FILE = TMP / "scaling_nodes.tsv"
EDGES_FILE = TMP / "scaling_edges.tsv"
SER_OUT = TMP / "scaling_serial.tsv"
PAR_OUT = TMP / "scaling_parallel.tsv"

N = 768
SET_COUNT = 96
SETS_PER_NODE = 4
ROUNDS = 8
SEARCH_STEPS = 80
REPEATS = 3
WORKERS_SWEEP = [1, 2, 4, 6, 8]


def run(cmd: List[str], cwd: Path) -> str:
    proc = subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(
            "Command failed:\n"
            + " ".join(cmd)
            + "\nSTDOUT:\n"
            + proc.stdout
            + "\nSTDERR:\n"
            + proc.stderr
        )
    return (proc.stdout + proc.stderr).strip()


def parse_elapsed_ms(text: str) -> float:
    m = re.search(r"elapsed_ms=([0-9]+(?:\.[0-9]+)?)", text)
    if not m:
        raise ValueError(f"Could not parse elapsed_ms from output: {text}")
    return float(m.group(1))


def parse_effective_workers(text: str) -> int:
    m = re.search(r"effective_workers=([0-9]+)", text)
    if not m:
        return 1
    return int(m.group(1))


def write_scaling_fixture() -> None:
    TMP.mkdir(exist_ok=True)

    with NODES_FILE.open("w", encoding="utf-8") as f:
        f.write("id\tfc\tset\n")
        for i in range(N):
            groups = [f"G{(i + j) % SET_COUNT}" for j in (0, 13, 27, 41)[:SETS_PER_NODE]]
            fc = 1.0 + ((i % 11) - 5) * 0.02
            f.write(f"n{i}\t{fc:.2f}\t{';'.join(groups)}\n")

    with EDGES_FILE.open("w", encoding="utf-8") as f:
        f.write("source\ttarget\tic\tec\n")
        for i in range(N):
            for j in range(1, 5):
                t = (i + j) % N
                f.write(f"n{i}\tn{t}\t0.2\t0.2\n")


def run_serial() -> str:
    return run(
        [
            "python3",
            "run_layout.py",
            "--nodes", str(NODES_FILE),
            "--edges", str(EDGES_FILE),
            "--output", str(SER_OUT),
            "--seed", "7",
            "--rounds", str(ROUNDS),
            "--ka", "1",
            "--kb", "-1",
            "--kc", "-1",
            "--search-steps", str(SEARCH_STEPS),
            "--eps", "1e-8",
            "--xrange", "100",
            "--yrange", "100",
        ],
        cwd=PY_BASE,
    )


def run_parallel(workers: int) -> str:
    return run(
        [
            "python3",
            "run_layout_parallel.py",
            "--nodes", str(NODES_FILE),
            "--edges", str(EDGES_FILE),
            "--output", str(PAR_OUT),
            "--seed", "7",
            "--rounds", str(ROUNDS),
            "--workers", str(workers),
            "--ka", "1",
            "--kb", "-1",
            "--kc", "-1",
            "--search-steps", str(SEARCH_STEPS),
            "--eps", "1e-8",
            "--xrange", "100",
            "--yrange", "100",
        ],
        cwd=PYP_BASE,
    )


def sample(fn, repeats: int = REPEATS) -> Tuple[float, List[float], str]:
    vals: List[float] = []
    last = ""
    for _ in range(repeats):
        last = fn()
        vals.append(parse_elapsed_ms(last))
    return statistics.mean(vals), vals, last


def main() -> None:
    write_scaling_fixture()

    run_serial()
    for w in WORKERS_SWEEP:
        run_parallel(w)

    serial_mean, serial_samples, serial_out = sample(run_serial)

    rows = []
    for w in WORKERS_SWEEP:
        mean_ms, samples, out = sample(lambda: run_parallel(w))
        eff = parse_effective_workers(out)
        speedup = serial_mean / mean_ms if mean_ms > 0 else 0.0
        rows.append((w, eff, mean_ms, samples, speedup))

    best = min(rows, key=lambda r: r[2])

    report_lines = [
        "# DEMA Scaling Benchmark Report",
        "",
        f"Generated: {dt.datetime.now().isoformat(timespec='seconds')}",
        "",
        "## Fixture",
        "",
        f"- Synthetic network: `{N}` nodes, `{N * 4}` directed edges",
        f"- Gene-set coverage: `{SET_COUNT}` unique sets, `{SETS_PER_NODE}` set memberships per node",
        f"- Solver config: rounds=`{ROUNDS}`, search_steps=`{SEARCH_STEPS}`, repeats=`{REPEATS}`",
        "",
        "## Serial Baseline",
        "",
        f"- Python(serial) mean ms: `{serial_mean:.3f}` samples={serial_samples}",
        f"- Last run: `{serial_out}`",
        "",
        "## Parallel Worker Sweep",
        "",
        "| requested_workers | effective_workers | mean_ms | speedup_vs_serial | samples |",
        "|---:|---:|---:|---:|---|",
    ]

    for w, eff, mean_ms, samples, speedup in rows:
        report_lines.append(f"| {w} | {eff} | {mean_ms:.3f} | {speedup:.3f}x | {samples} |")

    report_lines.extend(
        [
            "",
            "## Best Configuration",
            "",
            f"- Best requested workers: `{best[0]}`",
            f"- Effective workers used: `{best[1]}`",
            f"- Best mean runtime: `{best[2]:.3f} ms`",
            f"- Speedup vs serial: `{best[4]:.3f}x`",
            "",
            "## Notes",
            "",
            "- Parallel mode intentionally auto-caps effective workers to avoid over-threading regressions.",
            "- Small/medium fixtures often remain faster in serial mode due synchronization overhead.",
            "- Use this scaling report for worker tuning on larger production graphs.",
        ]
    )

    report = "\n".join(report_lines) + "\n"
    REPORT.write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
