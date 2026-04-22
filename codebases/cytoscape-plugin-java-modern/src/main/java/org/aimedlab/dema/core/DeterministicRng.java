package org.aimedlab.dema.core;

public final class DeterministicRng {
    private long state;

    public DeterministicRng(long seed) {
        this.state = seed;
    }

    public double nextDouble() {
        state = state * 6364136223846793005L + 1442695040888963407L;
        long bits = (state >>> 11) & ((1L << 53) - 1);
        return bits * 0x1.0p-53;
    }
}
