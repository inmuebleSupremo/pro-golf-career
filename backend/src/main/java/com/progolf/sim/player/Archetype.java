package com.progolf.sim.player;

/**
 * The starting archetype chosen at career creation (REQ-004). Identity records which archetype a golfer
 * began from; the actual starting-attribute generation lives in the population domain. Archetype is
 * immutable after creation.
 */
public enum Archetype {
    GRASS_ROOTS_TALENT(16, 20),
    TOP_COLLEGE_GRADUATE(21, 22),
    FUTURE_PRODIGY(16, 18);

    private final int minStartAge;
    private final int maxStartAge;

    Archetype(int minStartAge, int maxStartAge) {
        this.minStartAge = minStartAge;
        this.maxStartAge = maxStartAge;
    }

    /** Inclusive minimum starting age for this archetype. */
    public int minStartAge() {
        return minStartAge;
    }

    /** Inclusive maximum starting age for this archetype. */
    public int maxStartAge() {
        return maxStartAge;
    }
}
