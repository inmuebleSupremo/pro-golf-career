package com.progolf.sim.media;

/**
 * The accumulated career facts a {@link NarrativeClassifier} reads to derive a {@link CareerNarrative}
 * (spec: career-narrative). Supplied by the World from career/ranking history plus the media feed's
 * last-win tracking. {@code rankingPosition} and {@code seasonsSinceLastWin} use {@link Integer#MAX_VALUE}
 * to mean "unranked" and "never won".
 */
public record CareerSummary(int age, int seasonsPlayed, int careerWins, int rankingPosition, int seasonsSinceLastWin) {
}
