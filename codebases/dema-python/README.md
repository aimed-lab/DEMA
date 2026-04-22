# DEMA Python (Serial)

Pure-Python deterministic DEMA implementation that mirrors the modern Java core.

## What this update accomplished

- Re-implemented the DEMA core in Python with deterministic initialization and fixed-seed reproducibility.
- Matched Java core behavior exactly for the serial path on shared benchmark fixtures.
- Standardized TSV I/O so Python and Java can be benchmarked with identical inputs.
- Added cross-codebase synchronization via shared fixtures.

The serial Python implementation is now the reference path for Python correctness and reproducibility.

## Accuracy status

- On `benchmark-small` (seed `7`), Python(serial) vs Java max coordinate delta is `0.000000000000e+00`.
- This confirms exact parity for the serial Python path under the current benchmark harness.

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
- Python(serial) mean runtime: `256.273 ms` (latest benchmark run in this repository).
- Java mean runtime: `14.646 ms` (for context from same run).

## Hardware guidance

- Low-core laptops (2 to 4 cores): serial Python is usually the best default for small to medium graphs due to lower parallel overhead.
- Mid/high-core CPUs (8+ cores): still start with serial for parity-critical runs; evaluate parallel only when graph size is large enough to amortize coordination overhead.
- GPU environments: the current Python implementation is CPU-only (no GPU kernel path yet).

## Run

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
