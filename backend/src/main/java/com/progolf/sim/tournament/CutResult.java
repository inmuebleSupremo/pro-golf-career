package com.progolf.sim.tournament;

/**
 * The recorded result of the cut evaluation (REQ-093). {@code applied} is false for formats without a
 * cut. {@code cutLineScore} is the highest (worst) relative-to-par score that still advanced.
 */
public record CutResult(boolean applied, int cutLineScore, int madeCutCount) {
}
