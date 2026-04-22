.PHONY: help sync check benchmark all java-cli py-cli pyp-cli clean

help:
	@echo "Available targets:"
	@echo "  make sync       - synchronize shared fixtures into all maintained codebases"
	@echo "  make check      - run original plugin compatibility probe"
	@echo "  make benchmark  - run Java/Python/Parallel benchmark and parity report"
	@echo "  make all        - run sync + check + benchmark"
	@echo "  make java-cli   - run Java core CLI on benchmark-small"
	@echo "  make py-cli     - run serial Python layout on benchmark-small"
	@echo "  make pyp-cli    - run parallel Python layout on benchmark-small"
	@echo "  make clean      - remove temporary benchmark outputs"

sync:
	./scripts/sync_codebases.sh

check:
	./scripts/check_original_plugin_compat.sh

benchmark:
	./benchmarks/run_benchmark.sh

all: sync check benchmark

java-cli:
	cd codebases/cytoscape-plugin-java-modern && \
	mkdir -p build/classes && \
	javac -d build/classes $$(find src/main/java/org/aimedlab/dema/core src/main/java/org/aimedlab/dema/benchmark -name '*.java') && \
	java -cp build/classes org.aimedlab.dema.benchmark.DemaCoreCli \
	  --nodes fixtures/benchmark-small/nodes.tsv \
	  --edges fixtures/benchmark-small/edges.tsv \
	  --seed 7 \
	  --output /tmp/dema_java.tsv

py-cli:
	cd codebases/dema-python && \
	python3 run_layout.py \
	  --nodes fixtures/benchmark-small/nodes.tsv \
	  --edges fixtures/benchmark-small/edges.tsv \
	  --seed 7 \
	  --output /tmp/dema_py.tsv

pyp-cli:
	cd codebases/dema-python-parallel && \
	python3 run_layout_parallel.py \
	  --nodes fixtures/benchmark-small/nodes.tsv \
	  --edges fixtures/benchmark-small/edges.tsv \
	  --workers 4 \
	  --seed 7 \
	  --output /tmp/dema_py_parallel.tsv

clean:
	rm -rf .bench_tmp
