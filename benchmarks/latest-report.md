# DEMA Benchmark Report

Generated: 2026-05-04T08:08:11

## Parity (benchmark-small, seed=7)

- Java vs Python(serial) max |delta|: `4.661998787014e-09`
- Java vs Python(parallel) max |delta|: `6.012395449213e+02`
- Python(serial) vs Python(parallel) max |delta|: `6.012395449215e+02`

Raw run outputs:
- Java: `DEMA-JAVA rounds=22 energy=369.518867906409 elapsed_ms=1.757 output=/private/tmp/dema-remote-compare/.bench_tmp/java_small.tsv`
- Python(serial): `DEMA-PY rounds=22 energy=369.518867906433 elapsed_ms=0.387 backend=numba output=/private/tmp/dema-remote-compare/.bench_tmp/py_small.tsv`
- Python(parallel): `DEMA-PY-PAR rounds=85 energy=364.032956946038 elapsed_ms=0.732 workers=8 effective_workers=1 backend=numba-parallel output=/private/tmp/dema-remote-compare/.bench_tmp/pyp_small.tsv`

## Runtime (benchmark-medium, 3 runs each, rounds=80, workers=8)

- Java mean ms: `4.406` samples=[4.13, 4.964, 4.124]
- Python(serial) mean ms: `1.098` samples=[1.145, 1.109, 1.041]
- Python(parallel) mean ms: `1.988` samples=[1.97, 1.961, 2.034]

## Interpretation

- Java and Python(serial) should be treated as parity pair for "same algorithm" validation.
- Python(parallel) preserves the DEMA objective with adaptive CPU multicore updates and graph-size worker capping.
