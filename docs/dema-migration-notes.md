# DEMA Migration Notes

## Original repository check

The original repository (`codebases/original-js`) includes a historical JAR (`sample-custom-layout-1.0.jar`) and source files that do not build reproducibly as-is.

Validation script:

```bash
./scripts/check_original_plugin_compat.sh
```

Key findings:
- The JAR imports Cytoscape packages in version range `[3.0,4)`, so it targets Cytoscape 3.x APIs.
- The JAR was built with Java 8-era tooling.
- Source/package mismatch exists (`wzylayout.java` source package differs from class package inside the shipped JAR), which blocks clean maintenance from the checked-in source.

## Maintained codebases

- `codebases/cytoscape-plugin-java-modern`: maintained Cytoscape plugin codebase.
- `codebases/dema-python`: deterministic serial Python implementation.
- `codebases/dema-python-parallel`: multi-core oriented parallel Python variant.

## Synchronization

All three are synchronized via shared fixtures:

```bash
./scripts/sync_codebases.sh
```

## Benchmark/parity

Run benchmark + parity report:

```bash
./benchmarks/run_benchmark.sh
```

Report output:
- `benchmarks/latest-report.md`
