package org.cytoscape.sample.internal;

import java.util.Random;
import java.util.Set;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.AbstractLayoutAlgorithm;
import org.cytoscape.view.layout.AbstractLayoutTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.sample.internal.NormalContext;

public class CustomLayout extends AbstractLayoutAlgorithm {
	/**
	 * Creates a new MyLayout object.
	 */
	public CustomLayout(UndoSupport undo) {
		super("customLayout","DEMA1", undo);
	}
	
	public TaskIterator createTaskIterator(CyNetworkView networkView, Object context, Set<View<CyNode>> nodesToLayOut, String attrName) {
		final NormalContext myContext = (NormalContext) context;

		return new TaskIterator(new NormalTask(toString(), networkView, nodesToLayOut, attrName, undoSupport, myContext));
	}

	public Object createLayoutContext() {
		return new NormalContext();
	}
}
