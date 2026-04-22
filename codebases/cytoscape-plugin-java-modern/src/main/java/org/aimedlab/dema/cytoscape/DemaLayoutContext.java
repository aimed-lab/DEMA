package org.aimedlab.dema.cytoscape;

import org.cytoscape.work.Tunable;

public final class DemaLayoutContext {
    @Tunable(description = "Parameter KA")
    public int ka = 1;

    @Tunable(description = "Parameter Kb/Ka (set -1 for auto=node count)")
    public int kb = -1;

    @Tunable(description = "Parameter KC (set -1 for auto=10*node count when sets exist)")
    public int kc = -1;

    @Tunable(description = "Max iterations")
    public int maxRounds = 200;

    @Tunable(description = "Line-search steps")
    public int searchSteps = 100;

    @Tunable(description = "Numerical epsilon")
    public double eps = 1e-8;

    @Tunable(description = "X Range")
    public int xRange = 100;

    @Tunable(description = "Y Range")
    public int yRange = 100;

    @Tunable(description = "Random seed")
    public long seed = 7L;
}
