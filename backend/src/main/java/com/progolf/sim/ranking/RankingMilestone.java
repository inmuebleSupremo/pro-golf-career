package com.progolf.sim.ranking;

/**
 * Prestige ranking milestones (REQ-149), ordered from least to most prestigious. Each has the numerically
 * highest position that still counts as reaching it.
 */
public enum RankingMilestone {
    TOP_100(100),
    TOP_50(50),
    TOP_10(10),
    WORLD_NUMBER_ONE(1);

    private final int maxPosition;

    RankingMilestone(int maxPosition) {
        this.maxPosition = maxPosition;
    }

    public int maxPosition() {
        return maxPosition;
    }

    /** The most prestigious milestone satisfied by {@code position}, or null if outside the Top 100. */
    public static RankingMilestone forPosition(int position) {
        RankingMilestone best = null;
        for (RankingMilestone m : values()) {
            if (position <= m.maxPosition) {
                best = m; // values() is least-to-most prestigious; the last satisfied is the best
            }
        }
        return best;
    }
}
