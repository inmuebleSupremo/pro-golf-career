package com.progolf.sim.career;

/**
 * The Hall-of-Fame evaluation (spec: career-legacy) — a two-phase system. Phase one is baseline
 * eligibility ({@link #meetsBaseline}): a status gate (old enough OR long-enough retired) AND a
 * statistical gate (enough professional-tour wins AND enough majors). Phase two is the biennial election
 * the World runs, which ranks the baseline-eligible by a prestige-weighted {@link #score} and inducts the
 * top candidate. Every function here is a pure function of the supplied credentials and mutates nothing.
 */
public final class HallOfFame {

    private HallOfFame() {
    }

    /**
     * Phase one — baseline (ballot) eligibility: a golfer is nominable only if they meet BOTH a status
     * condition (competitive age at least {@code HOF_MIN_AGE} OR retired at least {@code HOF_RETIRED_SEASONS}
     * seasons) AND the statistical baseline (at least {@code HOF_MIN_PRO_WINS} professional wins AND at least
     * {@code HOF_MIN_MAJORS} majors).
     */
    public static boolean meetsBaseline(HallOfFameCredentials c) {
        boolean status = c.age() >= CareerConstants.HOF_MIN_AGE
                || c.seasonsSinceRetirement() >= CareerConstants.HOF_RETIRED_SEASONS;
        boolean statistics = c.proWins() >= CareerConstants.HOF_MIN_PRO_WINS
                && c.majorsWon() >= CareerConstants.HOF_MIN_MAJORS;
        return status && statistics;
    }

    /**
     * Phase two — the prestige-weighted career score the election ranks candidates by: majors are weighted
     * far above high-importance (signature) events, which are above regular professional wins, which are
     * above development-tier (amateur) wins.
     */
    public static double score(HallOfFameCredentials c) {
        return CareerConstants.HOF_SCORE_MAJOR * c.majorsWon()
                + CareerConstants.HOF_SCORE_SIGNATURE * c.signatureWins()
                + CareerConstants.HOF_SCORE_REGULAR * c.regularProWins()
                + CareerConstants.HOF_SCORE_DEVELOPMENT * c.developmentWins();
    }

    /**
     * The baseline-eligibility result a career records at retirement (spec: career-legacy). Induction
     * itself happens only through the World's biennial election, not here.
     */
    public static HallOfFameResult baselineResult(HallOfFameCredentials c) {
        boolean eligible = meetsBaseline(c);
        String summary = "proWins=" + c.proWins() + ", majors=" + c.majorsWon()
                + ", signature=" + c.signatureWins()
                + (eligible ? " — Hall-of-Fame baseline met" : " — below Hall-of-Fame baseline");
        return new HallOfFameResult(eligible, summary);
    }
}
