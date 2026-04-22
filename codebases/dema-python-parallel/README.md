# DEMA Python (Parallel)

Multi-core CPU implementation of DEMA using process-based parallel updates.

## What this update accomplished

- Added a parallel DEMA execution path that uses worker-based synchronous updates.
- Kept file format and CLI arguments aligned with serial Python and Java codebases.
- Added safe fallback behavior for restricted environments where process pools are blocked (falls back to threads).
- Integrated into the same benchmark and fixture system as the serial implementation.

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
- Python(parallel, workers=8) mean runtime: `1046.870 ms`
- Python(serial) mean runtime in same run: `256.273 ms`

Worker sweep on this machine (same fixture/rounds):
- workers=1: mean `868.854 ms`
- workers=2: mean `794.323 ms`
- workers=4: mean `1243.660 ms`
- workers=8: mean `803.474 ms`
- workers=10: mean `1007.766 ms`

Interpretation:
- In restricted environments that force thread fallback, parallel overhead can dominate and be slower than serial.
- On unrestricted process-enabled systems and larger graphs, parallel mode may still provide better wall-clock performance.

## Hardware guidance

- 2 to 4 cores: start with serial unless graph sizes are large.
- 8 to 16 cores: test parallel with workers near `core_count - 1` and compare against serial on your real dataset.
- GPU systems: no CUDA/ROCm path is implemented yet; current parallelization is CPU-only.

## Run

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
