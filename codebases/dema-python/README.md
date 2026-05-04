# DEMA Python (Serial)

Pure-Python deterministic DEMA implementation that mirrors the modern Java core.

## What this update accomplished

- Re-implemented the DEMA core in Python with deterministic initialization and fixed-seed reproducibility.
- Matched Java core behavior exactly for the serial path on shared benchmark fixtures.
- Standardized TSV I/O so Python and Java can be benchmarked with identical inputs.
- Added cross-codebase synchronization via shared fixtures.
- Added an accelerated Numba backend for hot loops, with automatic fallback to NumPy if Numba is unavailable.
- Added sparse set-membership indexing in core kernels so large gene-set workloads avoid dense set scans.

The serial Python implementation is now the reference path for Python correctness and reproducibility.

## Accuracy status

- On `benchmark-small` (seed `7`), Python(serial) vs Java max coordinate delta is `4.661998787014e-09`.
- This is numerically equivalent for practical use and keeps cross-language parity intact.

See the latest report: `benchmarks/latest-report.md`.

## Performance summary (measured)

Test machine:
- Apple M4
- 10 CPU cores
- 24 GB RAM
- Python 3.13.2

Benchmark configuration:
- Fixture: `benchmark-medium`
- Rounds: `80`
- Repeats: `3`

Results:
- Python(serial, backend=`numba`) mean runtime: `1.098 ms` (latest benchmark run in this repository).
- Java mean runtime: `4.406 ms` (for context from same run).

Large synthetic scaling run (`768` nodes, `3072` edges, `96` sets, `4` memberships/node):
- Python(serial) mean runtime: `122.371 ms`.
- Best parallel run on same fixture: `65.697 ms` (`1.863x` faster, see `benchmarks/scaling-report.md`).

Important timing note:
- Reported `elapsed_ms` is steady-state solver runtime.
- The CLI warms JIT before timing (`DemaEngine.warmup_jit()`), so one-time compilation cost is excluded from `elapsed_ms`.
- If Numba is not installed, the engine automatically falls back to a slower NumPy path.

## Hardware guidance

- Low-core laptops (2 to 4 cores): serial Python is usually the best default for small to medium graphs due to lower parallel overhead.
- Mid/high-core CPUs (8+ cores): still start with serial for parity-critical runs; evaluate parallel only when graph size is large enough to amortize coordination overhead.
- GPU environments: the current Python implementation is CPU-only (no GPU kernel path yet).

## Run

Install from this directory when you want the package and CLI on your Python path:

```bash
python3 -m pip install -e .
dema-py \
  --nodes fixtures/benchmark-small/nodes.tsv \
  --edges fixtures/benchmark-small/edges.tsv \
  --seed 7 \
  --output /tmp/dema_py.tsv
```

Or run directly from the source checkout:

```bash
python3 run_layout.py \
  --nodes fixtures/benchmark-small/nodes.tsv \
  --edges fixtures/benchmark-small/edges.tsv \
  --seed 7 \
  --output /tmp/dema_py.tsv
```

## Sync fixtures

From repository root:

```bash
./scripts/sync_codebases.sh
```
