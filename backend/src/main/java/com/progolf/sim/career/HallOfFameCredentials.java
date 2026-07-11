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
        boolean retired) {

    /** Professional-tour wins: total wins excluding development-tier (amateur) wins. */
    public int proWins() {
        return totalWins - developmentWins;
    }

    /** Regular professional wins: pro wins that are neither majors nor signature events. */
    public int regularProWins() {
        return totalWins - majorsWon - signatureWins - developmentWins;
    }

    /** Builds credentials from a career's statistics plus the caller-supplied age / retirement context. */
    public static HallOfFameCredentials of(CareerStatistics stats, int age, int seasonsSinceRetirement,
                                           boolean retired) {
        return new HallOfFameCredentials(stats.majorsWon(), stats.signatureWins(), stats.wins(),
                stats.developmentWins(), age, seasonsSinceRetirement, retired);
    }
}
