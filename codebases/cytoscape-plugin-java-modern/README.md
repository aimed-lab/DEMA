# DEMA Cytoscape Plugin (Modernized)

This codebase is the maintained Cytoscape plugin fork intended for Cytoscape 3.10.x.

## What changed vs original

- Reorganized into a standard Maven bundle project.
- Migrated source into consistent packages (`org.aimedlab.dema.*`).
- Isolated the DEMA math core for deterministic cross-language parity tests.
- Added a CLI entry point for benchmark parity with Python implementations.

## Build plugin JAR

Prerequisites:
- Java 17
- Maven 3

Build:

```bash
mvn -U clean package
```

The app bundle is produced in `target/` and can be installed in Cytoscape via:

`Apps -> App Store -> Install Apps from File...`

## Run Java parity CLI (no Cytoscape required)

```bash
mkdir -p build/classes
javac -d build/classes \
  $(find src/main/java/org/aimedlab/dema/core src/main/java/org/aimedlab/dema/benchmark -name '*.java')

java -cp build/classes org.aimedlab.dema.benchmark.DemaCoreCli \
  --nodes fixtures/benchmark-small/nodes.tsv \
  --edges fixtures/benchmark-small/edges.tsv \
  --seed 7 \
  --output /tmp/dema_java.tsv
```

## Layout columns recognized in Cytoscape

Node table columns:
- `name` (string, optional fallback to SUID)
- `FC` (double, optional, default `1.0`)
- `set` (string, optional comma-separated set names)
- `color` (string, optional `blue|red|green|pink`)

Edge table columns:
- `IC` (double, optional)
- `EC` (double, optional)

## Sync fixtures

Use the repository root script to sync shared fixtures into this codebase:

```bash
./scripts/sync_codebases.sh
```
