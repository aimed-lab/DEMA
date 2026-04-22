package org.aimedlab.dema.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TsvGraphLoader {
    private TsvGraphLoader() {
    }

    public static GraphData load(Path nodePath, Path edgePath) throws IOException {
        List<String> nodeLines = Files.readAllLines(nodePath, StandardCharsets.UTF_8);
        if (nodeLines.size() < 2) {
            throw new IllegalArgumentException("nodes.tsv must include a header and at least one node");
        }

        List<String> nodeIds = new ArrayList<>();
        List<Double> fcList = new ArrayList<>();
        Map<String, Integer> idToIndex = new LinkedHashMap<>();
        Map<String, List<Integer>> setToNodes = new LinkedHashMap<>();

        for (int i = 1; i < nodeLines.size(); i++) {
            String line = nodeLines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\t", -1);
            if (parts.length < 1) {
                continue;
            }

            String id = parts[0].trim();
            if (id.isEmpty()) {
                continue;
            }
            if (idToIndex.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate node id: " + id);
            }

            int idx = nodeIds.size();
            idToIndex.put(id, idx);
            nodeIds.add(id);

            double fc = 1.0;
            if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                fc = Double.parseDouble(parts[1].trim());
            }
            fcList.add(fc);

            if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
                String[] setNames = parts[2].split(",");
                for (String setNameRaw : setNames) {
                    String setName = setNameRaw.trim();
                    if (setName.isEmpty()) {
                        continue;
                    }
                    setToNodes.computeIfAbsent(setName, k -> new ArrayList<>()).add(idx);
                }
            }
        }

        int n = nodeIds.size();
        boolean[][] adjacency = new boolean[n][n];
        double[][] ic = new double[n][n];
        double[][] ec = new double[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(ic[i], Double.NaN);
            Arrays.fill(ec[i], Double.NaN);
        }

        List<String> edgeLines = Files.readAllLines(edgePath, StandardCharsets.UTF_8);
        if (edgeLines.size() < 2) {
            throw new IllegalArgumentException("edges.tsv must include a header and at least one edge");
        }
        for (int i = 1; i < edgeLines.size(); i++) {
            String line = edgeLines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\t", -1);
            if (parts.length < 2) {
                continue;
            }
            String sourceId = parts[0].trim();
            String targetId = parts[1].trim();
            Integer a = idToIndex.get(sourceId);
            Integer b = idToIndex.get(targetId);
            if (a == null || b == null) {
                throw new IllegalArgumentException("Edge references unknown node: " + sourceId + " -> " + targetId);
            }
            adjacency[a][b] = true;
            adjacency[b][a] = true;

            if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
                double v = Double.parseDouble(parts[2].trim());
                ic[a][b] = v;
                ic[b][a] = v;
            }
            if (parts.length >= 4 && !parts[3].trim().isEmpty()) {
                double v = Double.parseDouble(parts[3].trim());
                ec[a][b] = v;
                ec[b][a] = v;
            }
        }

        double[] fc = new double[n];
        for (int i = 0; i < n; i++) {
            fc[i] = fcList.get(i);
        }

        List<int[]> sets = new ArrayList<>();
        for (List<Integer> members : setToNodes.values()) {
            int[] m = new int[members.size()];
            for (int i = 0; i < members.size(); i++) {
                m[i] = members.get(i);
            }
            sets.add(m);
        }

        return new GraphData(nodeIds, adjacency, fc, ic, ec, sets);
    }
}
