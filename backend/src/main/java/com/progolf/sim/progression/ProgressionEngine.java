package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.util.List;
import java.util.Map;

/**
 * The pure progression engine (REQ-151–163): computes evolved {@link Attributes} from current attributes,
 * age, and a Development-Point allocation. No randomness and no side effects — the same inputs always
 * yield the same result, and it never touches a Player or a tournament result.
 */
public final class ProgressionEngine {

    private ProgressionEngine() {
    }

    /**
     * Applies one season of development: awards Development Points for the age's career stage, allocates
     * them via the AI policy, and raises attributes (gradual, capped, diminishing returns). Returns the
     * developed attributes.
     */
    public static Attributes develop(Attributes current, int age) {
        return develop(current, age, 1.0);
    }

    /**
     * Applies one season of development with a development-support factor (REQ-197): the awarded
     * Development Points are scaled by {@code supportFactor} (1.0 = unsupported) before allocation. A
     * coach raises this factor; attributes still change only through the sanctioned, capped allocation.
     */
    public static Attributes develop(Attributes current, int age, double supportFactor) {
        CareerStage stage = CareerStage.of(age);
        int points = (int) Math.round(DevelopmentPoints.award(stage) * Math.max(0.0, supportFactor));
        Map<Attribute, Integer> allocation = AllocationPolicy.aiAllocate(current, points);
        return applyAllocation(current, allocation);
    }

    /**
     * Applies one season of development directed by a player-chosen {@code focus} (spec: player-development):
     * the season's Development Points (scaled by {@code supportFactor}) are spent on the focus attributes in
     * priority order under the same per-season cap and cost curve as the automatic allocation. An empty or
     * null focus delegates to the automatic allocation, so unfocused development is unchanged.
     */
    public static Attributes develop(Attributes current, int age, double supportFactor, List<Attribute> focus) {
        if (focus == null || focus.isEmpty()) {
            return develop(current, age, supportFactor);
        }
        int points = (int) Math.round(DevelopmentPoints.award(CareerStage.of(age)) * Math.max(0.0, supportFactor));
        Attributes result = current;
        double totalGained = 0;
        for (Attribute a : focus) {
            int rating = result.get(a);
            while (points > 0 && rating < Attributes.MAX
                    && totalGained < ProgressionConstants.MAX_DEVELOPMENT_PER_SEASON) {
                int cost = DevelopmentPoints.costToRaise(rating);
                if (points < cost) {
                    break;
                }
                points -= cost;
                rating += 1;
                totalGained += 1;
            }
            if (rating != result.get(a)) {
                result = result.with(a, rating);
            }
        }
        return result;
    }

    /** Applies a specific Development-Point allocation to attributes (used by the player-driven path). */
    public static Attributes applyAllocation(Attributes current, Map<Attribute, Integer> allocation) {
        Attributes result = current;
        double totalGained = 0;
        for (Attribute a : Attribute.values()) { // deterministic order
            int points = allocation.getOrDefault(a, 0);
            int rating = result.get(a);
            while (points > 0 && rating < Attributes.MAX
                    && totalGained < ProgressionConstants.MAX_DEVELOPMENT_PER_SEASON) {
                int cost = DevelopmentPoints.costToRaise(rating);
                if (points < cost) {
                    break;
                }
                points -= cost;
                rating += 1;
                totalGained += 1;
            }
            if (rating != result.get(a)) {
                result = result.with(a, rating);
            }
        }
        return result;
    }

    /** Applies one season of attribute-specific aging, returning the aged attributes (clamped). */
    public static Attributes age(Attributes current, int age) {
        Attributes result = current;
        for (Attribute a : Attribute.values()) {
            int delta = AgingCurves.seasonDelta(a, age);
            if (delta != 0) {
                result = result.with(a, result.get(a) + delta); // Attributes.with clamps to 0-100
            }
        }
        return result;
    }

    /** A simple overall-ability read (mean attribute), for tracing a career trajectory. */
    public static double overallAbility(Attributes attributes) {
        double sum = 0;
        for (Attribute a : Attribute.values()) {
            sum += attributes.get(a);
        }
        return sum / Attribute.values().length;
    }
}
