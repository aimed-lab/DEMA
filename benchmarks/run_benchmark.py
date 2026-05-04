#!/usr/bin/env python3
from __future__ import annotations

import csv
import datetime as dt
import math
import os
import re
import shutil
import statistics
import subprocess
from pathlib import Path
from typing import Dict, List, Tuple

ROOT = Path(__file__).resolve().parents[1]
TMP = ROOT / ".bench_tmp"
REPORT = ROOT / "benchmarks" / "latest-report.md"

JAVA_BASE = ROOT / "codebases" / "cytoscape-plugin-java-modern"
PY_BASE = ROOT / "codebases" / "dema-python"
PYP_BASE = ROOT / "codebases" / "dema-python-parallel"

SMALL = JAVA_BASE / "fixtures" / "benchmark-small"
MEDIUM = JAVA_BASE / "fixtures" / "benchmark-medium"


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


def read_tsv(path: Path) -> Dict[str, Tuple[float, float]]:
    out: Dict[str, Tuple[float, float]] = {}
    with path.open("r", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for row in reader:
            out[row["id"]] = (float(row["x"]), float(row["y"]))
    return out


def max_abs_delta(a: Dict[str, Tuple[float, float]], b: Dict[str, Tuple[float, float]]) -> float:
    keys = sorted(set(a.keys()) | set(b.keys()))
    delta = 0.0
    for k in keys:
        if k not in a or k not in b:
            return math.inf
        ax, ay = a[k]
        bx, by = b[k]
        delta = max(delta, abs(ax - bx), abs(ay - by))
    return delta


def compile_java_core() -> None:
    classes_dir = JAVA_BASE / "build" / "classes"
    if classes_dir.exists():
        shutil.rmtree(classes_dir)
    classes_dir.mkdir(parents=True, exist_ok=True)

    java_files = [str(p) for p in (JAVA_BASE / "src" / "main" / "java").rglob("*.java")
                  if "/cytoscape/" not in str(p)]
    run(["javac", "-d", str(classes_dir)] + java_files, cwd=JAVA_BASE)


def run_java(nodes: Path, edges: Path, out_file: Path, rounds: int) -> str:
    return run(
        [
            "java",
            "-cp",
            str(JAVA_BASE / "build" / "classes"),
            "org.aimedlab.dema.benchmark.DemaCoreCli",
            "--nodes", str(nodes),
            "--edges", str(edges),
            "--output", str(out_file),
            "--seed", "7",
            "--rounds", str(rounds),
            "--ka", "1",
            "--kb", "-1",
            "--kc", "-1",
            "--search-steps", "100",
            "--eps", "1e-8",
            "--xrange", "100",
            "--yrange", "100",
        ],
        cwd=JAVA_BASE,
    )


def run_py(nodes: Path, edges: Path, out_file: Path, rounds: int) -> str:
    return run(
        [
            "python3", "run_layout.py",
            "--nodes", str(nodes),
            "--edges", str(edges),
            "--output", str(out_file),
            "--seed", "7",
            "--rounds", str(rounds),
            "--ka", "1",
            "--kb", "-1",
            "--kc", "-1",
            "--search-steps", "100",
            "--eps", "1e-8",
            "--xrange", "100",
            "--yrange", "100",
        ],
        cwd=PY_BASE,
    )


def run_py_parallel(nodes: Path, edges: Path, out_file: Path, rounds: int, workers: int) -> str:
    return run(
        [
            "python3", "run_layout_parallel.py",
            "--nodes", str(nodes),
            "--edges", str(edges),
            "--output", str(out_file),
            "--seed", "7",
            "--rounds", str(rounds),
            "--workers", str(workers),
            "--ka", "1",
            "--kb", "-1",
            "--kc", "-1",
            "--search-steps", "100",
            "--eps", "1e-8",
            "--xrange", "100",
            "--yrange", "100",
        ],
        cwd=PYP_BASE,
    )


def benchmark_runs(label: str, fn, repeats: int = 3) -> Tuple[float, List[float]]:
    values: List[float] = []
    for _ in range(repeats):
        out = fn()
        values.append(parse_elapsed_ms(out))
    return statistics.mean(values), values


def main() -> None:
    TMP.mkdir(exist_ok=True)
    compile_java_core()

    workers = max(2, min(8, (os.cpu_count() or 4) - 1))

    java_small_file = TMP / "java_small.tsv"
    py_small_file = TMP / "py_small.tsv"
    pyp_small_file = TMP / "pyp_small.tsv"

    java_small = run_java(SMALL / "nodes.tsv", SMALL / "edges.tsv", java_small_file, rounds=120)
    py_small = run_py(SMALL / "nodes.tsv", SMALL / "edges.tsv", py_small_file, rounds=120)
    pyp_small = run_py_parallel(SMALL / "nodes.tsv", SMALL / "edges.tsv", pyp_small_file, rounds=120, workers=workers)

    java_small_map = read_tsv(java_small_file)
    py_small_map = read_tsv(py_small_file)
    pyp_small_map = read_tsv(pyp_small_file)

    delta_java_py = max_abs_delta(java_small_map, py_small_map)
    delta_java_pyp = max_abs_delta(java_small_map, pyp_small_map)
    delta_py_pyp = max_abs_delta(py_small_map, pyp_small_map)

    medium_rounds = 80
    java_medium_file = TMP / "java_medium.tsv"
    py_medium_file = TMP / "py_medium.tsv"
    pyp_medium_file = TMP / "pyp_medium.tsv"

    java_mean, java_samples = benchmark_runs(
        "java",
        lambda: run_java(MEDIUM / "nodes.tsv", MEDIUM / "edges.tsv", java_medium_file, rounds=medium_rounds),
    )
    py_mean, py_samples = benchmark_runs(
        "py",
        lambda: run_py(MEDIUM / "nodes.tsv", MEDIUM / "edges.tsv", py_medium_file, rounds=medium_rounds),
    )
    pyp_mean, pyp_samples = benchmark_runs(
        "py_parallel",
        lambda: run_py_parallel(MEDIUM / "nodes.tsv", MEDIUM / "edges.tsv", pyp_medium_file, rounds=medium_rounds, workers=workers),
    )

    report = f"""# DEMA Benchmark Report

Generated: {dt.datetime.now().isoformat(timespec='seconds')}

## Parity (benchmark-small, seed=7)

- Java vs Python(serial) max |delta|: `{delta_java_py:.12e}`
- Java vs Python(parallel) max |delta|: `{delta_java_pyp:.12e}`
- Python(serial) vs Python(parallel) max |delta|: `{delta_py_pyp:.12e}`

Raw run outputs:
- Java: `{java_small}`
- Python(serial): `{py_small}`
- Python(parallel): `{pyp_small}`

## Runtime (benchmark-medium, 3 runs each, rounds={medium_rounds}, workers={workers})

- Java mean ms: `{java_mean:.3f}` samples={java_samples}
- Python(serial) mean ms: `{py_mean:.3f}` samples={py_samples}
- Python(parallel) mean ms: `{pyp_mean:.3f}` samples={pyp_samples}

## Interpretation

- Java and Python(serial) should be treated as parity pair for "same algorithm" validation.
- Python(parallel) preserves the DEMA objective with adaptive CPU multicore updates and graph-size worker capping.
"""

    REPORT.write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
