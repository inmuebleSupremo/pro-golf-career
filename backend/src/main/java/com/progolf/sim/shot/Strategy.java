package com.progolf.sim.shot;

/**
 * The three Version 1 shot strategies (REQ-055). Strategy shapes the risk/reward of the outcome
 * distribution — a higher dispersion multiplier means more spread (greater potential reward and
 * greater risk) — but it never alters how attributes contribute.
 */
public enum Strategy {
    CONSERVATIVE(0.80),
    BALANCED(1.00),
    AGGRESSIVE(1.30);

    private final double dispersionMultiplier;

    Strategy(double dispersionMultiplier) {
        this.dispersionMultiplier = dispersionMultiplier;
    }

    /** Multiplier applied to shot dispersion (sigma) for this strategy. */
    public double dispersionMultiplier() {
        return dispersionMultiplier;
    }
}
