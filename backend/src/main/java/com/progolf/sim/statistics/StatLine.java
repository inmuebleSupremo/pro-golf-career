package com.progolf.sim.statistics;

/**
 * An accumulated competitive stat line (spec: competitive-statistics, REQ-251). Immutable: {@link #plus}
 * folds an outcome or another line into a new line, so the same type serves both per-season accumulation
 * and career aggregation. {@code bestFinish} is {@link Integer#MAX_VALUE} when no counted event exists;
 * {@code totalScoreVsPar} is cumulative strokes relative to par across counted events (lower is better).
 */
public record StatLine(int events, int cuts, int wins, int runnerUps, int topTens,
                       int bestFinish, int totalScoreVsPar, double earnings) {

    /** An empty stat line (the accumulation identity). */
    public static StatLine empty() {
        return new StatLine(0, 0, 0, 0, 0, Integer.MAX_VALUE, 0, 0.0);
    }

    /** The stat line contributed by a single outcome. */
    public static StatLine of(EventOutcome o) {
        boolean counts = o.counts();
        int events = counts ? 1 : 0;
        int cuts = counts && o.madeCut() ? 1 : 0;
        int wins = counts && o.position() == 1 ? 1 : 0;
        int runnerUps = counts && o.position() == 2 ? 1 : 0;
        int topTens = counts && o.position() <= StatisticsConstants.TOP_N ? 1 : 0;
        int bestFinish = counts ? o.position() : Integer.MAX_VALUE;
        int score = counts ? o.scoreVsPar() : 0;
        return new StatLine(events, cuts, wins, runnerUps, topTens, bestFinish, score, o.prize());
    }

    /** Folds one outcome into this line. */
    public StatLine plus(EventOutcome o) {
        return plus(of(o));
    }

    /** Folds another line into this one. */
    public StatLine plus(StatLine o) {
        return new StatLine(
                events + o.events,
                cuts + o.cuts,
                wins + o.wins,
                runnerUps + o.runnerUps,
                topTens + o.topTens,
                Math.min(bestFinish, o.bestFinish),
                totalScoreVsPar + o.totalScoreVsPar,
                earnings + o.earnings);
    }

    /** Mean score relative to par across counted events (0 when none). Lower is better. */
    public double scoringAverage() {
        return events == 0 ? 0.0 : (double) totalScoreVsPar / events;
    }

    /** Fraction of counted events in which the cut was made (0 when none). */
    public double cutMakeRate() {
        return events == 0 ? 0.0 : (double) cuts / events;
    }
}
