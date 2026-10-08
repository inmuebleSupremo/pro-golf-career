package com.progolf.sim.shot;

/** Pure shape eligibility; short-game and bunker techniques remain straight-only. */
public final class ShotShapeEligibility {
    private ShotShapeEligibility() { }

    public static ShotFamilyEligibility.Result evaluate(ShotFamily family, ShotShape shape) {
        if (shape == ShotShape.STRAIGHT || family == ShotFamily.FULL || family == ShotFamily.CONTROLLED) {
            return ShotFamilyEligibility.Result.permitted();
        }
        return ShotFamilyEligibility.Result.rejected(family + " shots are STRAIGHT-only");
    }
}
