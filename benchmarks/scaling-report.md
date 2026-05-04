# DEMA Scaling Benchmark Report

Generated: 2026-05-04T08:08:26

## Fixture

- Synthetic network: `768` nodes, `3072` directed edges
- Gene-set coverage: `96` unique sets, `4` set memberships per node
- Solver config: rounds=`8`, search_steps=`80`, repeats=`3`

## Serial Baseline

- Python(serial) mean ms: `122.371` samples=[122.119, 120.996, 123.999]
- Last run: `DEMA-PY rounds=8 energy=1955388.836164103355 elapsed_ms=123.999 backend=numba output=/private/tmp/dema-remote-compare/.bench_tmp/scaling_serial.tsv`

## Parallel Worker Sweep

| requested_workers | effective_workers | mean_ms | speedup_vs_serial | samples |
|---:|---:|---:|---:|---|
| 1 | 1 | 112.154 | 1.091x | [113.317, 111.989, 111.155] |
| 2 | 2 | 81.043 | 1.510x | [82.372, 80.345, 80.413] |
| 4 | 4 | 71.871 | 1.703x | [75.716, 68.4, 71.497] |
| 6 | 6 | 66.347 | 1.844x | [66.425, 66.873, 65.743] |
| 8 | 6 | 65.697 | 1.863x | [66.102, 65.165, 65.825] |

## Best Configuration

- Best requested workers: `8`
- Effective workers used: `6`
- Best mean runtime: `65.697 ms`
- Speedup vs serial: `1.863x`

## Notes

- Parallel mode intentionally auto-caps effective workers to avoid over-threading regressions.
- Small/medium fixtures often remain faster in serial mode due synchronization overhead.
- Use this scaling report for worker tuning on larger production graphs.
