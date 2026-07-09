package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
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
        CareerStage stage = CareerStage.of(age);
        int points = DevelopmentPoints.award(stage);
        Map<Attribute, Integer> allocation = AllocationPolicy.aiAllocate(current, points);
        return applyAllocation(current, allocation);
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
