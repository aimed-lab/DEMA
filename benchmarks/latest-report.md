# DEMA Benchmark Report

Generated: 2026-04-22T01:04:13

## Parity (benchmark-small, seed=7)

- Java vs Python(serial) max |delta|: `4.661998787014e-09`
- Java vs Python(parallel) max |delta|: `6.012395449213e+02`
- Python(serial) vs Python(parallel) max |delta|: `6.012395449215e+02`

Raw run outputs:
- Java: `DEMA-JAVA rounds=22 energy=369.518867906409 elapsed_ms=5.975 output=/Users/jakechen/Library/CloudStorage/Box-Box/My Profession/My Projects/Current/DEMA/codebases/original-js/.bench_tmp/java_small.tsv`
- Python(serial): `DEMA-PY rounds=22 energy=369.518867906433 elapsed_ms=4.206 backend=numba output=/Users/jakechen/Library/CloudStorage/Box-Box/My Profession/My Projects/Current/DEMA/codebases/original-js/.bench_tmp/py_small.tsv`
- Python(parallel): `DEMA-PY-PAR rounds=85 energy=364.032956946038 elapsed_ms=13.325 workers=8 effective_workers=1 backend=numba-parallel output=/Users/jakechen/Library/CloudStorage/Box-Box/My Profession/My Projects/Current/DEMA/codebases/original-js/.bench_tmp/pyp_small.tsv`

## Runtime (benchmark-medium, 3 runs each, rounds=80, workers=8)

- Java mean ms: `14.351` samples=[13.901, 13.771, 15.38]
- Python(serial) mean ms: `2.264` samples=[1.978, 2.863, 1.952]
- Python(parallel) mean ms: `11.334` samples=[10.118, 14.217, 9.666]

## Interpretation

- Java and Python(serial) should be treated as parity pair for "same algorithm" validation.
- Python(parallel) preserves the DEMA objective but uses synchronous process-parallel updates for multi-core execution.
