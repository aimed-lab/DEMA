# DEMA Python (Parallel)

Multi-core CPU implementation of DEMA using a compiled Numba parallel kernel (`prange`) for synchronous node updates.

## What this update accomplished

- Added a compiled Numba-parallel execution path for synchronous node updates.
- Kept file format and CLI arguments aligned with serial Python and Java codebases.
- Added safe fallback behavior when Numba is unavailable (falls back to NumPy/Python path).
- Integrated into the same benchmark and fixture system as the serial implementation.
- Added automatic worker capping based on graph size to avoid over-threading regressions on small graphs.

This codebase is focused on throughput experiments and multi-core execution, while the serial codebase remains the strict parity reference.

## Accuracy and parity notes

- The parallel solver preserves the DEMA objective but does not produce strict coordinate parity with the serial solver.
- On `benchmark-small` (seed `7`), Python(parallel) differs from Java/serial output, as expected for parallel synchronous updates.

See `benchmarks/latest-report.md` for current parity deltas.

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

Latest benchmark run in this repository:
- Python(parallel, workers=8) mean runtime: `11.334 ms`
- Python(serial, backend=`numba`) mean runtime in same run: `2.264 ms`
- Java mean runtime in same run: `14.351 ms`

Current interpretation:
- Parallel is now dramatically faster than the previous process-dispatch implementation.
- For small/medium graphs, accelerated serial can still be faster due synchronization costs.
- Parallel is intended for larger workloads where multicore throughput can amortize per-round coordination.

Backend note:
- CLI output includes `backend=` and `effective_workers=`.
- `workers` is requested concurrency; `effective_workers` is auto-capped per graph size.

## Hardware guidance

- 2 to 4 cores: start with serial unless graph sizes are large.
- 8 to 16 cores: test parallel with workers near `core_count - 1` and compare against serial on your real dataset.
- GPU systems: no CUDA/ROCm path is implemented yet; current parallelization is CPU-only.

## Run

Install from this directory when you want the package and CLI on your Python path:

```bash
python3 -m pip install -e .
dema-py-parallel \
  --nodes fixtures/benchmark-small/nodes.tsv \
  --edges fixtures/benchmark-small/edges.tsv \
  --workers 4 \
  --seed 7 \
  --output /tmp/dema_py_parallel.tsv
```

Or run directly from the source checkout:

```bash
python3 run_layout_parallel.py \
  --nodes fixtures/benchmark-small/nodes.tsv \
  --edges fixtures/benchmark-small/edges.tsv \
  --workers 4 \
  --seed 7 \
  --output /tmp/dema_py_parallel.tsv
```

## Sync fixtures

From repository root:

```bash
./scripts/sync_codebases.sh
```
