package org.aimedlab.dema.benchmark;

import org.aimedlab.dema.core.DemaLayoutEngine;
import org.aimedlab.dema.core.GraphData;
import org.aimedlab.dema.core.LayoutParams;
import org.aimedlab.dema.core.LayoutResult;
import org.aimedlab.dema.core.TsvGraphLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DemaCoreCli {
    private DemaCoreCli() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> kv = parseArgs(args);

        Path nodes = requiredPath(kv, "--nodes");
        Path edges = requiredPath(kv, "--edges");
        Path output = requiredPath(kv, "--output");

        GraphData graph = TsvGraphLoader.load(nodes, edges);

        int ka = intArg(kv, "--ka", 1);
        int kbRaw = intArg(kv, "--kb", -1);
        int kb = kbRaw < 0 ? graph.nodeCount() : kbRaw;
        int kcRaw = intArg(kv, "--kc", -1);
        int kc = kcRaw < 0 ? (graph.sets.isEmpty() ? 0 : graph.nodeCount() * 10) : kcRaw;
        int rounds = intArg(kv, "--rounds", 200);
        int search = intArg(kv, "--search-steps", 100);
        double eps = doubleArg(kv, "--eps", 1e-8);
        int xRange = intArg(kv, "--xrange", 100);
        int yRange = intArg(kv, "--yrange", 100);
        long seed = longArg(kv, "--seed", 7L);

        LayoutParams params = new LayoutParams(ka, kb, kc, rounds, search, eps, xRange, yRange, seed);

        long startNs = System.nanoTime();
        DemaLayoutEngine engine = new DemaLayoutEngine(graph, params);
        LayoutResult result = engine.run();
        long elapsedNs = System.nanoTime() - startNs;

        writeResult(output, graph, result);

        System.out.printf(Locale.US,
                "DEMA-JAVA rounds=%d energy=%.12f elapsed_ms=%.3f output=%s%n",
                result.rounds,
                result.finalEnergy,
                elapsedNs / 1_000_000.0,
                output.toAbsolutePath());
    }

    private static void writeResult(Path output, GraphData graph, LayoutResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id\tx\ty\n");
        for (int i = 0; i < graph.nodeCount(); i++) {
            sb.append(graph.nodeIds.get(i)).append('\t')
              .append(String.format(Locale.US, "%.12f", result.positions[i][0])).append('\t')
              .append(String.format(Locale.US, "%.12f", result.positions[i][1]))
              .append('\n');
        }
        Files.writeString(output, sb.toString(), StandardCharsets.UTF_8);
    }

    private static Path requiredPath(Map<String, String> kv, String key) {
        String v = kv.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        return Path.of(v);
    }

    private static int intArg(Map<String, String> kv, String key, int defaultValue) {
        String v = kv.get(key);
        return v == null ? defaultValue : Integer.parseInt(v);
    }

    private static long longArg(Map<String, String> kv, String key, long defaultValue) {
        String v = kv.get(key);
        return v == null ? defaultValue : Long.parseLong(v);
    }

    private static double doubleArg(Map<String, String> kv, String key, double defaultValue) {
        String v = kv.get(key);
        return v == null ? defaultValue : Double.parseDouble(v);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> out = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String k = args[i];
            if (!k.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected token: " + k);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for argument: " + k);
            }
            String v = args[++i];
            out.put(k, v);
        }
        return out;
    }
}
