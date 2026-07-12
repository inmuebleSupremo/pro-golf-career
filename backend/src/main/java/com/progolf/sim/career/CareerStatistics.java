package com.progolf.sim.career;

/**
 * Cumulative career statistics (REQ-032), folded from tournament results. Mutation is package-private
 * (only the {@link Career} folds into it); reads are public. Average finish is exact via a running
 * sum-of-finishes over counted (non-withdrawn) finishes.
 */
public final class CareerStatistics {

    private int eventsPlayed;
    private int countedFinishes;
    private int cutsMade;
    private int wins;
    private int majorsWon;
    private int signatureWins;
    private int developmentWins;
    private int runnerUps;
    private int topTens;
    private long sumOfFinishes;
    private double totalEarnings;

    /** Folds one result finish for the golfer into the running totals (a non-classified win). */
    void recordResult(int position, boolean madeCut, boolean withdrawn, double prize) {
        recordResult(position, madeCut, withdrawn, prize, WinCategory.REGULAR);
    }

    /**
     * Folds one result finish for the golfer into the running totals (spec: event-prestige, career-legacy).
     * A win is additionally classified — major / signature / development-tier — into disjoint legacy
     * buckets (regular pro wins are the remainder), a permanent part of the career's Hall-of-Fame credentials.
     */
    void recordResult(int position, boolean madeCut, boolean withdrawn, double prize, WinCategory winCategory) {
        eventsPlayed++;
        totalEarnings += prize;
        if (withdrawn) {
            return; // an event was played, but a withdrawal has no finishing result
        }
        countedFinishes++;
        sumOfFinishes += position;
        if (madeCut) {
            cutsMade++;
        }
        if (position == 1) {
            wins++;
            switch (winCategory) {
                case MAJOR -> majorsWon++;
                case SIGNATURE -> signatureWins++;
                case DEVELOPMENT -> developmentWins++;
                case REGULAR, NONE -> { /* a regular professional win, counted in the total only */ }
            }
        } else if (position == 2) {
            runnerUps++;
        }
        if (position <= CareerConstants.TOP_10) {
            topTens++;
        }
    }

    public int eventsPlayed() {
        return eventsPlayed;
    }

    public int cutsMade() {
        return cutsMade;
    }

    public int wins() {
        return wins;
    }

    /** Majors won over the career (spec: event-prestige) — the marquee accomplishment. */
    public int majorsWon() {
        return majorsWon;
    }

    /** High-importance (signature) event wins over the career (spec: career-legacy). */
    public int signatureWins() {
        return signatureWins;
    }

    /** Development-tier (amateur) wins over the career (spec: career-legacy) — the lightest credential. */
    public int developmentWins() {
        return developmentWins;
    }

    /** Professional-tour wins: total wins excluding development-tier (amateur) wins (spec: career-legacy). */
    public int proWins() {
        return wins - developmentWins;
    }

    public int runnerUps() {
        return runnerUps;
    }

    public int topTens() {
        return topTens;
    }

    public double totalEarnings() {
        return totalEarnings;
    }

    /** Mean of the golfer's counted finishing positions, or 0 if none. */
    public double averageFinish() {
        return countedFinishes == 0 ? 0.0 : (double) sumOfFinishes / countedFinishes;
    }

    /** An immutable capture of the cumulative statistics (spec: world-snapshot). */
    public record Snapshot(int eventsPlayed, int countedFinishes, int cutsMade, int wins, int majorsWon,
                           int signatureWins, int developmentWins, int runnerUps, int topTens,
                           long sumOfFinishes, double totalEarnings) {
    }

    public Snapshot snapshot() {
        return new Snapshot(eventsPlayed, countedFinishes, cutsMade, wins, majorsWon, signatureWins,
                developmentWins, runnerUps, topTens, sumOfFinishes, totalEarnings);
    }

    /** Restores the cumulative totals in place from a snapshot (spec: world-snapshot). */
    void restoreFrom(Snapshot s) {
        eventsPlayed = s.eventsPlayed();
        countedFinishes = s.countedFinishes();
        cutsMade = s.cutsMade();
        wins = s.wins();
        majorsWon = s.majorsWon();
        signatureWins = s.signatureWins();
        developmentWins = s.developmentWins();
        runnerUps = s.runnerUps();
        topTens = s.topTens();
        sumOfFinishes = s.sumOfFinishes();
        totalEarnings = s.totalEarnings();
    }
}
