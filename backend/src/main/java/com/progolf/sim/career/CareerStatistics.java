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
    private int runnerUps;
    private int topTens;
    private long sumOfFinishes;
    private double totalEarnings;

    /** Folds one result finish for the golfer into the running totals. */
    void recordResult(int position, boolean madeCut, boolean withdrawn, double prize) {
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
