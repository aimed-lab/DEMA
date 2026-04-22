package org.aimedlab.dema.core;

import java.util.Collections;
import java.util.List;

public final class GraphData {
    public final List<String> nodeIds;
    public final boolean[][] adjacency;
    public final double[] foldChanges;
    public final double[][] ic;
    public final double[][] ec;
    public final List<int[]> sets;

    public GraphData(
            List<String> nodeIds,
            boolean[][] adjacency,
            double[] foldChanges,
            double[][] ic,
            double[][] ec,
            List<int[]> sets
    ) {
        this.nodeIds = Collections.unmodifiableList(nodeIds);
        this.adjacency = adjacency;
        this.foldChanges = foldChanges;
        this.ic = ic;
        this.ec = ec;
        this.sets = Collections.unmodifiableList(sets);
    }

    public int nodeCount() {
        return nodeIds.size();
    }
}
