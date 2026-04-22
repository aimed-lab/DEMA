#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ORIG_DIR="$ROOT_DIR"
JAR_PATH="$ORIG_DIR/plugin/sample-custom-layout-1.0.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Missing original plugin JAR: $JAR_PATH" >&2
  exit 1
fi

MANIFEST="$(unzip -p "$JAR_PATH" META-INF/MANIFEST.MF)"
IMPORT_LINE="$(printf '%s\n' "$MANIFEST" | tr -d '\r' | grep -E '^Import-Package:' || true)"
BUILD_JDK="$(printf '%s\n' "$MANIFEST" | tr -d '\r' | grep -E '^Build-Jdk:' || true)"

SRC_WZY_PACKAGE="$(grep -n '^package ' "$ORIG_DIR/code/internal/wzylayout.java" | head -n1 || true)"
CLASS_WZY_PATH="$(jar tf "$JAR_PATH" | grep 'wzylayout.class' | head -n1 || true)"

echo "Original DEMA plugin compatibility probe"
echo "- JAR path: $JAR_PATH"
echo "- Build JDK: ${BUILD_JDK#Build-Jdk: }"
echo "- Import packages: ${IMPORT_LINE#Import-Package: }"
echo "- Source package declaration: $SRC_WZY_PACKAGE"
echo "- Built class path: $CLASS_WZY_PATH"

if [[ "$IMPORT_LINE" == *'version="[3.0,4)"'* ]]; then
  echo "- Cytoscape API range in manifest includes Cytoscape 3.x."
else
  echo "- WARNING: Cytoscape API range could not be confirmed for 3.x."
fi

if [[ "$SRC_WZY_PACKAGE" == *'package wzylayout;'* ]] && [[ "$CLASS_WZY_PATH" == 'org/cytoscape/sample/internal/wzylayout.class' ]]; then
  echo "- WARNING: Source package does not match shipped class package (repo source is not reproducible as-is)."
fi
