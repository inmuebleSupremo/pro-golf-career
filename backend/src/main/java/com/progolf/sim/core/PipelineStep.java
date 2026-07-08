package com.progolf.sim.core;

/**
 * The fixed seven-step calculation pipeline every Outcome-producing calculation follows (REQ-044).
 *
 * <p>The declaration order IS the calculation order and must never be skipped or reordered. Steps
 * {@link #BASE_ATTRIBUTE}..{@link #ENVIRONMENTAL_EFFECTS} shape the probability distribution;
 * {@link #CONTROLLED_RANDOMNESS} samples it; {@link #SAFETY_NET} bounds the sample; and
 * {@link #FINAL_OUTCOME} emits the immutable result.
 */
public enum PipelineStep {
    BASE_ATTRIBUTE,
    PERMANENT_CAREER_EFFECTS,
    TEMPORARY_MODIFIERS,
    ENVIRONMENTAL_EFFECTS,
    CONTROLLED_RANDOMNESS,
    SAFETY_NET,
    FINAL_OUTCOME;

    /** True if this step runs strictly before {@code other} in the pipeline. */
    public boolean isBefore(PipelineStep other) {
        return this.ordinal() < other.ordinal();
    }
}
