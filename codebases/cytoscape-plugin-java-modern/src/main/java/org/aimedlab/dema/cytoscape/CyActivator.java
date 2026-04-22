package org.aimedlab.dema.cytoscape;

import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;

import java.util.Properties;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;

public final class CyActivator extends AbstractCyActivator {
    @Override
    public void start(BundleContext bc) {
        UndoSupport undo = getService(bc, UndoSupport.class);

        DemaLayoutAlgorithm algorithm = new DemaLayoutAlgorithm(undo);
        Properties props = new Properties();
        props.setProperty(PREFERRED_MENU, "Custom Layouts");

        registerService(bc, algorithm, CyLayoutAlgorithm.class, props);
    }
}
