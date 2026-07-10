package com.progolf.sim.career;

/**
 * Hall-of-Fame eligibility evaluation (REQ-036). The thresholds are placeholders in
 * {@link CareerConstants}; a later Legacy Systems specification refines the real criteria. Evaluation is
 * a pure function of the career's recorded statistics and never mutates anything.
 */
public final class HallOfFame {

    private HallOfFame() {
    }

    /** Evaluates eligibility from a career's cumulative statistics. */
    public static HallOfFameResult evaluate(CareerStatistics stats) {
        int wins = stats.wins();
        int topTens = stats.topTens();
        int majors = stats.majorsWon();
        boolean byWins = wins >= CareerConstants.HOF_MIN_WINS;
        boolean byConsistency = wins >= CareerConstants.HOF_ALT_WINS && topTens >= CareerConstants.HOF_ALT_TOP_10S;
        boolean byMajors = majors >= CareerConstants.HOF_MIN_MAJORS; // majors are the marquee accomplishment
        boolean eligible = byWins || byConsistency || byMajors;
        String summary = "wins=" + wins + ", majors=" + majors + ", top10s=" + topTens
                + (eligible ? " — eligible" : " — not eligible") + " (placeholder criteria)";
        return new HallOfFameResult(eligible, summary);
    }
}
