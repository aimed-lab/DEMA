# DEMA Benchmark Report

Generated: 2026-04-22T00:51:23

## Parity (benchmark-small, seed=7)

- Java vs Python(serial) max |delta|: `4.661998787014e-09`
- Java vs Python(parallel) max |delta|: `3.681293071173e+02`
- Python(serial) vs Python(parallel) max |delta|: `3.681293071180e+02`

Raw run outputs:
- Java: `DEMA-JAVA rounds=22 energy=369.518867906409 elapsed_ms=5.617 output=/Users/jakechen/Library/CloudStorage/Box-Box/My Profession/My Projects/Current/DEMA/codebases/original-js/.bench_tmp/java_small.tsv`
- Python(serial): `DEMA-PY rounds=22 energy=369.518867906433 elapsed_ms=1.975 backend=numba output=/Users/jakechen/Library/CloudStorage/Box-Box/My Profession/My Projects/Current/DEMA/codebases/original-js/.bench_tmp/py_small.tsv`
- Python(parallel): `DEMA-PY-PAR rounds=95 energy=362.126012859306 elapsed_ms=162.499 workers=8 output=/Users/jakechen/Library/CloudStorage/Box-Box/My Profession/My Projects/Current/DEMA/codebases/original-js/.bench_tmp/pyp_small.tsv`

## Runtime (benchmark-medium, 3 runs each, rounds=80, workers=8)

- Java mean ms: `11.254` samples=[10.894, 9.145, 13.722]
- Python(serial) mean ms: `10.277` samples=[14.707, 7.032, 9.092]
- Python(parallel) mean ms: `681.839` samples=[612.013, 747.96, 685.544]

## Interpretation

- Java and Python(serial) should be treated as parity pair for "same algorithm" validation.
- Python(parallel) preserves the DEMA objective but uses synchronous process-parallel updates for multi-core execution.
