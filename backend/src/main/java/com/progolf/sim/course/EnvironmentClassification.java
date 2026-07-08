package com.progolf.sim.course;

/**
 * The six Version 1 course environment classifications (REQ-080). Classification is data other domains
 * (e.g. Weather) read to influence conditions and strategic character; the Course domain itself
 * generates no weather. The per-classification biases below only shape generation (exposure, hazard mix).
 */
public enum EnvironmentClassification {
    LINKS(0.90, 0.25, 0.05),
    PARKLAND(0.40, 0.20, 0.35),
    DESERT(0.55, 0.05, 0.05),
    MOUNTAIN(0.60, 0.15, 0.25),
    COASTAL(0.85, 0.35, 0.10),
    WOODLAND(0.35, 0.15, 0.55);

    private final double exposure;
    private final double waterBias;
    private final double treeBias;

    EnvironmentClassification(double exposure, double waterBias, double treeBias) {
        this.exposure = exposure;
        this.waterBias = waterBias;
        this.treeBias = treeBias;
    }

    /** Relative exposure to weather in [0,1]; read by other domains, not acted on here. */
    public double exposure() {
        return exposure;
    }

    /** Probability bias that a generated hole carries a water hazard. */
    public double waterBias() {
        return waterBias;
    }

    /** Probability bias that a generated hole is tree-lined. */
    public double treeBias() {
        return treeBias;
    }
}
