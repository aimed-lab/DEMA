package org.aimedlab.dema.core;

public final class LayoutResult {
    public final double[][] positions;
    public final int rounds;
    public final double finalEnergy;

    public LayoutResult(double[][] positions, int rounds, double finalEnergy) {
        this.positions = positions;
        this.rounds = rounds;
        this.finalEnergy = finalEnergy;
    }
}
