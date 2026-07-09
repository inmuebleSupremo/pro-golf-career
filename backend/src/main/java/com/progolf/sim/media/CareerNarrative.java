package com.progolf.sim.media;

/**
 * A descriptive career narrative label (spec: career-narrative, REQ-244/248), recognising diverse forms of
 * success. Descriptive, never prescriptive — it interprets a career, it does not shape it.
 */
public enum CareerNarrative {
    RISING_PROSPECT("A rising prospect climbing the world rankings"),
    BREAKTHROUGH_SEASON("Enjoying a breakthrough after early-career success"),
    CONSISTENT_CONTENDER("A consistent contender chasing a first title"),
    DOMINANT_CHAMPION("A dominant champion atop the world game"),
    VETERAN_RESURGENCE("A veteran enjoying a late-career resurgence"),
    CHAMPIONSHIP_DROUGHT("A former winner enduring a long championship drought"),
    ESTABLISHED_PROFESSIONAL("An established touring professional");

    private final String description;

    CareerNarrative(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
