package com.progolf.sim.shot;

/**
 * The dominant factors that shaped a resolved shot, captured for explainability (REQ-065). Each field
 * is a signed contribution where positive is favourable: {@code attribute} from the golfer's skill,
 * {@code environment} from wind/lie, {@code strategy} from the chosen risk level, and {@code luck} the
 * realised deviation from the expected result.
 */
public record FactorBreakdown(double attribute, double environment, double strategy, double luck) {
}
