package org.aimedlab.dema.core;

public final class LayoutParams {
    public final int ka;
    public final int kb;
    public final int kc;
    public final int maxRounds;
    public final int searchSteps;
    public final double eps;
    public final int xRange;
    public final int yRange;
    public final long seed;

    public LayoutParams(
            int ka,
            int kb,
            int kc,
            int maxRounds,
            int searchSteps,
            double eps,
            int xRange,
            int yRange,
            long seed
    ) {
        this.ka = ka;
        this.kb = kb;
        this.kc = kc;
        this.maxRounds = maxRounds;
        this.searchSteps = searchSteps;
        this.eps = eps;
        this.xRange = xRange;
        this.yRange = yRange;
        this.seed = seed;
    }
}
