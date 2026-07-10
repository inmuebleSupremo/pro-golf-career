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
    private int runnerUps;
    private int topTens;
    private long sumOfFinishes;
    private double totalEarnings;

    /** Folds one result finish for the golfer into the running totals (a non-major event). */
    void recordResult(int position, boolean madeCut, boolean withdrawn, double prize) {
        recordResult(position, madeCut, withdrawn, prize, false);
    }

    /**
     * Folds one result finish for the golfer into the running totals (spec: event-prestige). A win in a
     * major additionally increments majors won — a permanent part of the career's legacy.
     */
    void recordResult(int position, boolean madeCut, boolean withdrawn, double prize, boolean majorWin) {
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
            if (majorWin) {
                majorsWon++;
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
}
