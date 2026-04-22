package org.aimedlab.dema.cytoscape;

import java.util.Set;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.AbstractLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.undo.UndoSupport;

public final class DemaLayoutAlgorithm extends AbstractLayoutAlgorithm {
    public DemaLayoutAlgorithm(UndoSupport undoSupport) {
        super("demaLayout", "DEMA", undoSupport);
    }

    @Override
    public TaskIterator createTaskIterator(
            CyNetworkView networkView,
            Object context,
            Set<View<CyNode>> nodesToLayOut,
            String attrName
    ) {
        DemaLayoutContext ctx = (DemaLayoutContext) context;
        return new TaskIterator(new DemaLayoutTask(toString(), networkView, nodesToLayOut, attrName, undoSupport, ctx));
    }

    @Override
    public Object createLayoutContext() {
        return new DemaLayoutContext();
    }
}
