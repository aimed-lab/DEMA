# DEMA Benchmark Report

Generated: 2026-04-22T00:36:06

## Parity (benchmark-small, seed=7)

- Java vs Python(serial) max |delta|: `0.000000000000e+00`
- Java vs Python(parallel) max |delta|: `3.681293071173e+02`
- Python(serial) vs Python(parallel) max |delta|: `3.681293071173e+02`

Raw run outputs:
- Java: `DEMA-JAVA rounds=22 energy=369.518867906409 elapsed_ms=6.827 output=/Users/jakechen/Library/CloudStorage/Box-Box/My Profession/My Projects/Current/DEMA/codebases/original-js/.bench_tmp/java_small.tsv`
- Python(serial): `DEMA-PY rounds=22 energy=369.518867906409 elapsed_ms=25.272 output=/Users/jakechen/Library/CloudStorage/Box-Box/My Profession/My Projects/Current/DEMA/codebases/original-js/.bench_tmp/py_small.tsv`
- Python(parallel): `DEMA-PY-PAR rounds=95 energy=362.126012859306 elapsed_ms=80.054 workers=8 output=/Users/jakechen/Library/CloudStorage/Box-Box/My Profession/My Projects/Current/DEMA/codebases/original-js/.bench_tmp/pyp_small.tsv`

## Runtime (benchmark-medium, 3 runs each, rounds=80, workers=8)

- Java mean ms: `12.920` samples=[17.22, 12.727, 8.813]
- Python(serial) mean ms: `146.000` samples=[141.118, 154.732, 142.151]
- Python(parallel) mean ms: `1542.446` samples=[2650.722, 1314.541, 662.074]

## Interpretation

- Java and Python(serial) should be treated as parity pair for "same algorithm" validation.
- Python(parallel) preserves the DEMA objective but uses synchronous process-parallel updates for multi-core execution.
