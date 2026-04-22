package org.aimedlab.dema.core;

import java.util.ArrayList;
import java.util.List;

public final class DemaLayoutEngine {
    private final GraphData graph;
    private final LayoutParams params;
    private final int n;
    private final double[][] positions;
    private final double[][] weight;
    private final double[] search;
    private final List<boolean[]> setMembership;

    public DemaLayoutEngine(GraphData graph, LayoutParams params) {
        this.graph = graph;
        this.params = params;
        this.n = graph.nodeCount();
        this.positions = new double[n][2];
        this.weight = new double[n][n];
        this.search = buildSearchGrid(params.searchSteps);
        this.setMembership = buildSetMembership(graph.sets, n);

        computeWeight();
        randomInitialize();
    }

    public LayoutResult run() {
        int roundsCompleted = 0;
        for (int round = 0; round < params.maxRounds; round++) {
            if (anyConstraintViolation()) {
                throw new IllegalStateException("Distance constraint violation before round " + round);
            }

            double[][] setCenters = computeSetCenters();
            double before = totalEnergy(setCenters);

            for (int i = 0; i < n; i++) {
                double currentEnergy = nodeEnergy(i, setCenters);
                double[] grad = normalizedGradient(i, setCenters);
                if (grad == null) {
                    continue;
                }

                int stepIdx = binarySearchStep(1, params.searchSteps, i, grad[0], grad[1], currentEnergy, setCenters);
                positions[i][0] += grad[0] * search[stepIdx];
                positions[i][1] += grad[1] * search[stepIdx];
            }

            double after = totalEnergy(setCenters);
            roundsCompleted = round + 1;
            if (Math.abs(before - after) <= params.eps) {
                break;
            }
        }

        double[][] scaled = scaleToRange();
        double finalEnergy = totalEnergy(computeSetCenters());
        return new LayoutResult(scaled, roundsCompleted, finalEnergy);
    }

    private static List<boolean[]> buildSetMembership(List<int[]> sets, int nodeCount) {
        List<boolean[]> membership = new ArrayList<>(sets.size());
        for (int[] set : sets) {
            boolean[] flags = new boolean[nodeCount];
            for (int idx : set) {
                flags[idx] = true;
            }
            membership.add(flags);
        }
        return membership;
    }

    private static double[] buildSearchGrid(int steps) {
        double[] out = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            out[i] = i / (double) steps;
        }
        return out;
    }

    private void computeWeight() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (!graph.adjacency[i][j]) {
                    weight[i][j] = 0.0;
                    continue;
                }

                boolean hasIc = !Double.isNaN(graph.ic[i][j]);
                boolean hasEc = !Double.isNaN(graph.ec[i][j]);

                if (!hasIc && !hasEc) {
                    weight[i][j] = 1.0;
                } else if (!hasIc) {
                    weight[i][j] = transformedWeight(graph.ec[i][j]);
                } else if (!hasEc) {
                    weight[i][j] = transformedWeight(graph.ic[i][j]);
                } else {
                    double a = transformedDenominator(graph.ic[i][j]);
                    double b = transformedDenominator(graph.ec[i][j]);
                    double denom = a + b;
                    if (Math.abs(denom) < params.eps) {
                        denom = params.eps;
                    }
                    weight[i][j] = (1.0 / denom) + 1.0;
                }
                if (weight[i][j] < params.eps) {
                    weight[i][j] = params.eps;
                }
            }
        }
    }

    private double transformedWeight(double v) {
        double denom = transformedDenominator(v);
        if (Math.abs(denom) < params.eps) {
            denom = params.eps;
        }
        return (1.0 / denom) + 1.0;
    }

    private double transformedDenominator(double v) {
        double clamped = Math.max(-0.999999999, Math.min(0.999999999, v));
        return -Math.log((1.0 - clamped) / (1.0 + clamped));
    }

    private void randomInitialize() {
        DeterministicRng rng = new DeterministicRng(params.seed);

        for (int i = 0; i < n; i++) {
            boolean placed = false;
            int attempts = 0;

            while (!placed && attempts < 20000) {
                attempts++;

                double x = rng.nextDouble();
                double y = rng.nextDouble();
                double d = norm(x, y);
                if (d <= params.eps) {
                    continue;
                }

                x = x / (d * 4.0);
                y = y / (d * 4.0);

                positions[i][0] = x;
                positions[i][1] = y;

                placed = true;
                for (int j = 0; j < i; j++) {
                    double dis = distance(positions[i], positions[j]);
                    if (dis <= params.eps) {
                        placed = false;
                        break;
                    }
                    if (graph.adjacency[i][j] && dis >= weight[i][j]) {
                        placed = false;
                        break;
                    }
                }
            }

            if (!placed) {
                // deterministic fallback to avoid infinite loops on very constrained graphs
                positions[i][0] = (i + 1) / (double) (n + 1) / 4.0;
                positions[i][1] = (n - i) / (double) (n + 1) / 4.0;
            }
        }
    }

    private double[][] computeSetCenters() {
        double[][] centers = new double[graph.sets.size()][2];
        for (int s = 0; s < graph.sets.size(); s++) {
            int[] set = graph.sets.get(s);
            if (set.length == 0) {
                continue;
            }
            double x = 0.0;
            double y = 0.0;
            for (int node : set) {
                x += positions[node][0];
                y += positions[node][1];
            }
            centers[s][0] = x / set.length;
            centers[s][1] = y / set.length;
        }
        return centers;
    }

    private boolean anyConstraintViolation() {
        for (int i = 0; i < n; i++) {
            if (violatesNodeConstraint(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean violatesNodeConstraint(int node) {
        for (int j = 0; j < n; j++) {
            if (graph.adjacency[node][j]) {
                if (distance(positions[node], positions[j]) > weight[node][j]) {
                    return true;
                }
            }
        }
        return false;
    }

    private double nodeEnergy(int node, double[][] setCenters) {
        double e = 0.0;
        for (int i = 0; i < n; i++) {
            if (i == node) {
                continue;
            }
            double dis = Math.max(distance(positions[node], positions[i]), params.eps);
            e += params.ka * graph.foldChanges[node] * graph.foldChanges[i] / dis;

            if (graph.adjacency[node][i]) {
                double denom = Math.max(weight[node][i] - dis, params.eps);
                e += params.kb * graph.foldChanges[node] * graph.foldChanges[i] / denom;
            }
        }

        for (int s = 0; s < graph.sets.size(); s++) {
            if (setMembership.get(s)[node]) {
                double dis = distance(positions[node], setCenters[s]);
                e += dis * dis * params.kc;
            }
        }

        return e;
    }

    private double totalEnergy(double[][] setCenters) {
        double total = 0.0;
        for (int i = 0; i < n; i++) {
            total += nodeEnergy(i, setCenters);
        }
        return total / 2.0;
    }

    private double[] normalizedGradient(int node, double[][] setCenters) {
        double gx = 0.0;
        double gy = 0.0;

        for (int j = 0; j < n; j++) {
            if (j == node) {
                continue;
            }

            double dis = Math.max(distance(positions[node], positions[j]), params.eps);
            double dx = positions[j][0] - positions[node][0];
            double dy = positions[j][1] - positions[node][1];

            double repel = params.ka * graph.foldChanges[node] * graph.foldChanges[j] / (dis * dis);
            gx -= (dx / dis) * repel;
            gy -= (dy / dis) * repel;

            if (graph.adjacency[node][j]) {
                double denom = Math.max(weight[node][j] - dis, params.eps);
                double attract = params.kb * graph.foldChanges[node] * graph.foldChanges[j] / (denom * denom);
                gx += (dx / dis) * attract;
                gy += (dy / dis) * attract;
            }
        }

        for (int s = 0; s < graph.sets.size(); s++) {
            if (setMembership.get(s)[node]) {
                double dis = distance(positions[node], setCenters[s]);
                gx += (setCenters[s][0] - positions[node][0]) * 2.0 * dis * params.kc;
                gy += (setCenters[s][1] - positions[node][1]) * 2.0 * dis * params.kc;
            }
        }

        double norm = norm(gx, gy);
        if (norm <= params.eps || Double.isNaN(norm)) {
            return null;
        }
        return new double[]{gx / norm, gy / norm};
    }

    private int binarySearchStep(int left, int right, int node, double dx, double dy, double currentEnergy, double[][] setCenters) {
        if (left > right) {
            return 0;
        }

        int mid = (left + right) / 2;
        positions[node][0] += dx * search[mid];
        positions[node][1] += dy * search[mid];

        boolean improves = nodeEnergy(node, setCenters) < currentEnergy && !violatesNodeConstraint(node);

        positions[node][0] -= dx * search[mid];
        positions[node][1] -= dy * search[mid];

        if (improves) {
            int t = binarySearchStep(mid + 1, right, node, dx, dy, currentEnergy, setCenters);
            return t == 0 ? mid : t;
        }
        return binarySearchStep(left, mid - 1, node, dx, dy, currentEnergy, setCenters);
    }

    private double[][] scaleToRange() {
        if (n == 0) {
            return new double[0][2];
        }

        double xMin = positions[0][0];
        double xMax = positions[0][0];
        double yMin = positions[0][1];
        double yMax = positions[0][1];

        for (int i = 1; i < n; i++) {
            xMin = Math.min(xMin, positions[i][0]);
            xMax = Math.max(xMax, positions[i][0]);
            yMin = Math.min(yMin, positions[i][1]);
            yMax = Math.max(yMax, positions[i][1]);
        }

        double xMid = (xMin + xMax) / 2.0;
        double yMid = (yMin + yMax) / 2.0;
        double maxLen = Math.max(xMax - xMin, yMax - yMin);
        if (maxLen <= params.eps) {
            maxLen = 1.0;
        }

        double expandFactor = 0.1;
        double[][] scaled = new double[n][2];
        for (int i = 0; i < n; i++) {
            scaled[i][0] = params.xRange * (positions[i][0] - xMid) / maxLen / expandFactor;
            scaled[i][1] = params.yRange * (positions[i][1] - yMid) / maxLen / expandFactor;
        }
        return scaled;
    }

    private static double distance(double[] a, double[] b) {
        return norm(a[0] - b[0], a[1] - b[1]);
    }

    private static double norm(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }
}
