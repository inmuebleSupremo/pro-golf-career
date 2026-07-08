package com.progolf.sim.player;

/**
 * The single tunables surface for Player state behaviour (mirrors {@code SimConstants}). The Live Skill
 * Rating magnitudes are deliberately placeholder pending real tournament-performance coupling; only the
 * directional behaviour they produce is contractually required for now (REQ-019).
 */
public final class PlayerConstants {

    private PlayerConstants() {
    }

    // --- Live Skill Rating ---
    public static final double RATING_BASELINE = 50.0;
    public static final double RATING_MIN = 0.0;
    public static final double RATING_MAX = 100.0;
    /** Fraction of the gap to the baseline closed per inactivity step. */
    public static final double RATING_DECAY_RATE = 0.10;

    // --- Fatigue ---
    public static final double FATIGUE_MIN = 0.0;
    public static final double FATIGUE_MAX = 1.0;
}
