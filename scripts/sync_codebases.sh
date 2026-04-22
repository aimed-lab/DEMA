#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SHARED_FIXTURES="$ROOT_DIR/codebases/shared/fixtures"
TARGETS=(
  "$ROOT_DIR/codebases/cytoscape-plugin-java-modern"
  "$ROOT_DIR/codebases/dema-python"
  "$ROOT_DIR/codebases/dema-python-parallel"
)

for target in "${TARGETS[@]}"; do
  mkdir -p "$target/fixtures"
  rm -rf "$target/fixtures/benchmark-small" "$target/fixtures/benchmark-medium"
  cp -R "$SHARED_FIXTURES/benchmark-small" "$target/fixtures/"
  cp -R "$SHARED_FIXTURES/benchmark-medium" "$target/fixtures/"
  cp "$SHARED_FIXTURES/README.md" "$target/fixtures/README.md"
  echo "Synced fixtures -> $target/fixtures"
done
