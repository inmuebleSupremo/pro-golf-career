package com.progolf.sim.shot;

/**
 * The three Version 1 shot strategies (REQ-055). Strategy shapes the risk/reward of the outcome
 * distribution — a higher dispersion multiplier means more spread (greater potential reward and
 * greater risk) — but it never alters how attributes contribute.
 */
public enum Strategy {
    CONSERVATIVE(0.93, 0.0),
    BALANCED(1.00, 0.35),
    AGGRESSIVE(1.42, 1.0);

    private final double dispersionMultiplier;
    private final double pinAttack;

    Strategy(double dispersionMultiplier, double pinAttack) {
        this.dispersionMultiplier = dispersionMultiplier;
        this.pinAttack = pinAttack;
    }

    /** Multiplier applied to shot dispersion (sigma) for this strategy. */
    public double dispersionMultiplier() {
        return dispersionMultiplier;
    }

    /**
     * How much of the pin's offset this strategy aims at on a scoring approach (spec: shot-resolution
     * pin-attacking): 0 aims at the safe green centre, 1 fires straight at a tucked flag. Aggressive play
     * hunts the pin for birdie chances at the cost of the flanking hazard; conservative plays the centre.
     */
    public double pinAttack() {
        return pinAttack;
    }
}
