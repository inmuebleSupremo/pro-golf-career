package com.progolf.sim.career;

/**
 * The primitive inputs a Hall-of-Fame evaluation needs (spec: career-legacy), decoupling the pure
 * evaluator from where the data lives: the career supplies the achievement and age fields, while only the
 * World knows how many seasons have elapsed since a golfer retired. All fields are primitives so the
 * evaluator stays a pure function.
 */
public record HallOfFameCredentials(
        int majorsWon,
        int signatureWins,
        int totalWins,
        int developmentWins,
        int age,
        int seasonsSinceRetirement,
        boolean retired,
        int careerHighRanking,
        int seasonsAtNumberOne,
        double careerEarnings) {

    /** Backward-compatible constructor for callers/tests that do not supply ranking or earnings context. */
    public HallOfFameCredentials(int majorsWon, int signatureWins, int totalWins, int developmentWins,
                                 int age, int seasonsSinceRetirement, boolean retired) {
        this(majorsWon, signatureWins, totalWins, developmentWins, age, seasonsSinceRetirement, retired,
                CareerConstants.HOF_UNRANKED, 0, 0.0);
    }

    /** Professional-tour wins: total wins excluding development-tier (amateur) wins. */
    public int proWins() {
        return totalWins - developmentWins;
    }

    /** Regular professional wins: pro wins that are neither majors nor signature events. */
    public int regularProWins() {
        return totalWins - majorsWon - signatureWins - developmentWins;
    }

    /** Builds credentials from a career's statistics plus caller-supplied age/retirement context (no ranking). */
    public static HallOfFameCredentials of(CareerStatistics stats, int age, int seasonsSinceRetirement,
                                           boolean retired) {
        return of(stats, age, seasonsSinceRetirement, retired, CareerConstants.HOF_UNRANKED, 0);
    }

    /**
     * Builds credentials including ranking dominance (career-high position and seasons finishing at World
     * #1); earnings come from the career's statistics. Baseline eligibility ignores ranking/earnings; they
     * feed only the score.
     */
    public static HallOfFameCredentials of(CareerStatistics stats, int age, int seasonsSinceRetirement,
                                           boolean retired, int careerHighRanking, int seasonsAtNumberOne) {
        return new HallOfFameCredentials(stats.majorsWon(), stats.signatureWins(), stats.wins(),
                stats.developmentWins(), age, seasonsSinceRetirement, retired,
                careerHighRanking, seasonsAtNumberOne, stats.totalEarnings());
    }
}
