package com.progolf.sim.progression;

/**
 * Development Points (REQ-151): the resource earned each season and spent to raise attributes. Awards are
 * scaled by career stage; raising a higher rating costs more points (diminishing returns), which keeps
 * growth gradual and prevents numerical inflation (REQ-156).
 */
public final class DevelopmentPoints {

    private DevelopmentPoints() {
    }

    /** Development Points awarded for a completed season at a given career stage. */
    public static int award(CareerStage stage) {
        return (int) Math.round(ProgressionConstants.DP_PER_SEASON * stage.developmentMultiplier());
    }

    /** Points needed to raise an attribute currently at {@code rating} by one point. */
    public static int costToRaise(int rating) {
        double extra = Math.max(0, rating - ProgressionConstants.COST_REFERENCE);
        return (int) Math.round(ProgressionConstants.POINTS_PER_RATING * (1.0 + extra * ProgressionConstants.COST_GROWTH));
    }
}
