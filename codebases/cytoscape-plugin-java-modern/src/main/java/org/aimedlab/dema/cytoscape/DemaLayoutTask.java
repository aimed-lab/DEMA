package org.aimedlab.dema.cytoscape;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimedlab.dema.core.DemaLayoutEngine;
import org.aimedlab.dema.core.GraphData;
import org.aimedlab.dema.core.LayoutParams;
import org.aimedlab.dema.core.LayoutResult;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.layout.AbstractLayoutTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.undo.UndoSupport;

public final class DemaLayoutTask extends AbstractLayoutTask {
    private final DemaLayoutContext context;

    public DemaLayoutTask(
            String displayName,
            CyNetworkView networkView,
            Set<View<CyNode>> nodesToLayOut,
            String attrName,
            UndoSupport undoSupport,
            DemaLayoutContext context
    ) {
        super(displayName, networkView, nodesToLayOut, attrName, undoSupport);
        this.context = context;
    }

    @Override
    protected void doLayout(TaskMonitor taskMonitor) {
        taskMonitor.setTitle("DEMA layout");
        taskMonitor.setStatusMessage("Building DEMA matrices...");

        CyNetwork network = networkView.getModel();
        List<CyNode> nodes = network.getNodeList();
        int n = nodes.size();

        Map<Long, Integer> suidToIndex = new LinkedHashMap<>();
        List<String> nodeIds = new ArrayList<>(n);
        double[] fc = new double[n];
        Map<String, List<Integer>> setMap = new LinkedHashMap<>();
        String[] nodeColors = new String[n];

        for (int i = 0; i < n; i++) {
            CyNode node = nodes.get(i);
            suidToIndex.put(node.getSUID(), i);

            CyRow row = network.getRow(node);
            String name = row.get("name", String.class);
            nodeIds.add(name == null || name.isBlank() ? node.getSUID().toString() : name);

            Double fcValue = row.get("FC", Double.class);
            fc[i] = fcValue == null ? 1.0 : fcValue;

            String color = row.get("color", String.class);
            nodeColors[i] = color == null ? "blue" : color.trim().toLowerCase();

            String setCol = row.get("set", String.class);
            if (setCol != null && !setCol.isBlank()) {
                for (String token : setCol.split(",")) {
                    String setName = token.trim();
                    if (setName.isEmpty()) {
                        continue;
                    }
                    setMap.computeIfAbsent(setName, k -> new ArrayList<>()).add(i);
                }
            }
        }

        boolean[][] adjacency = new boolean[n][n];
        double[][] ic = new double[n][n];
        double[][] ec = new double[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(ic[i], Double.NaN);
            Arrays.fill(ec[i], Double.NaN);
        }

        for (CyEdge edge : network.getEdgeList()) {
            Integer a = suidToIndex.get(edge.getSource().getSUID());
            Integer b = suidToIndex.get(edge.getTarget().getSUID());
            if (a == null || b == null) {
                continue;
            }

            adjacency[a][b] = true;
            adjacency[b][a] = true;

            CyRow row = network.getRow(edge);
            Double icValue = row.get("IC", Double.class);
            Double ecValue = row.get("EC", Double.class);
            if (icValue != null) {
                ic[a][b] = icValue;
                ic[b][a] = icValue;
            }
            if (ecValue != null) {
                ec[a][b] = ecValue;
                ec[b][a] = ecValue;
            }
        }

        List<int[]> sets = new ArrayList<>();
        for (List<Integer> members : setMap.values()) {
            int[] arr = new int[members.size()];
            for (int i = 0; i < members.size(); i++) {
                arr[i] = members.get(i);
            }
            sets.add(arr);
        }

        GraphData graph = new GraphData(nodeIds, adjacency, fc, ic, ec, sets);

        int kb = context.kb < 0 ? n : context.kb;
        int kc = context.kc < 0 ? (sets.isEmpty() ? 0 : n * 10) : context.kc;

        LayoutParams params = new LayoutParams(
                context.ka,
                kb,
                kc,
                context.maxRounds,
                context.searchSteps,
                context.eps,
                context.xRange,
                context.yRange,
                context.seed
        );

        taskMonitor.setStatusMessage("Running DEMA optimization...");
        LayoutResult result = new DemaLayoutEngine(graph, params).run();

        taskMonitor.setStatusMessage("Applying node coordinates...");
        for (View<CyNode> nodeView : nodesToLayOut) {
            CyNode node = nodeView.getModel();
            Integer idx = suidToIndex.get(node.getSUID());
            if (idx == null) {
                continue;
            }

            nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, result.positions[idx][0]);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, result.positions[idx][1]);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, colorFor(nodeColors[idx]));
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_BORDER_PAINT, Color.BLACK);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_BORDER_WIDTH, 5.0);
        }

        taskMonitor.setStatusMessage("DEMA complete");
    }

    private static Color colorFor(String colorName) {
        return switch (colorName) {
            case "red" -> Color.RED;
            case "green" -> Color.GREEN;
            case "pink" -> Color.PINK;
            case "blue" -> Color.BLUE;
            default -> Color.BLUE;
        };
    }
}
