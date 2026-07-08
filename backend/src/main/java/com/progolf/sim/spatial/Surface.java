package com.progolf.sim.spatial;

/**
 * The Version 1 surface catalogue (REQ-071). Every zone band references exactly one surface, and each
 * surface declares a consistent influence on subsequent play: whether it is playable, whether it is a
 * penalty hazard, the strokes it adds, and a relative recovery difficulty in [0,1].
 */
public enum Surface {
    TEE_BOX(true, false, 0, 0.00),
    FAIRWAY(true, false, 0, 0.00),
    FIRST_CUT(true, false, 0, 0.10),
    PRIMARY_ROUGH(true, false, 0, 0.30),
    DEEP_ROUGH(true, false, 0, 0.60),
    GREEN(true, false, 0, 0.00),
    FRINGE(true, false, 0, 0.05),
    BUNKER(true, false, 0, 0.50),
    WASTE_AREA(true, false, 0, 0.40),
    RECOVERY_AREA(true, false, 0, 0.70),
    TREES(true, false, 0, 0.80),
    WATER(false, true, 1, 1.00),
    OUT_OF_BOUNDS(false, true, 1, 1.00);

    private final boolean playable;
    private final boolean hazard;
    private final int penaltyStrokes;
    private final double recoveryDifficulty;

    Surface(boolean playable, boolean hazard, int penaltyStrokes, double recoveryDifficulty) {
        this.playable = playable;
        this.hazard = hazard;
        this.penaltyStrokes = penaltyStrokes;
        this.recoveryDifficulty = recoveryDifficulty;
    }

    /** Whether a ball may be played from this surface without a penalty drop. */
    public boolean isPlayable() {
        return playable;
    }

    /** Whether landing here incurs a penalty (Water, Out of Bounds). */
    public boolean isHazard() {
        return hazard;
    }

    /** Penalty strokes added when a shot finishes on this surface. */
    public int penaltyStrokes() {
        return penaltyStrokes;
    }

    /** Relative difficulty of the next shot from this surface, in [0,1]. */
    public double recoveryDifficulty() {
        return recoveryDifficulty;
    }
}
