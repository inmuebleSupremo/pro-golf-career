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
     * them via the AI policy, and raises attributes toward {@code potential} (gradual, capped, diminishing
     * returns). Returns the developed attributes.
     */
    public static Attributes develop(Attributes current, Attributes potential, int age) {
        return develop(current, potential, age, 1.0);
    }

    /**
     * Applies one season of development with a development-support factor (REQ-197): the awarded
     * Development Points are scaled by {@code supportFactor} (1.0 = unsupported) before allocation. A
     * coach raises this factor; attributes still change only through the sanctioned, capped allocation.
     */
    public static Attributes develop(Attributes current, Attributes potential, int age, double supportFactor) {
        Map<Attribute, Integer> allocation = AllocationPolicy.aiAllocate(current, potential, pointsFor(age, supportFactor));
        return applyAllocation(current, potential, allocation);
    }

    /**
     * Applies one season of development directed by a player-chosen {@code focus} (spec: player-development):
     * the season's Development Points (scaled by {@code supportFactor}) are spent on the focus attributes in
     * priority order under the same per-season cap and cost curve as the automatic allocation. An empty or
     * null focus delegates to the automatic allocation, so unfocused development is unchanged.
     */
    public static Attributes develop(Attributes current, Attributes potential, int age, double supportFactor,
                                     List<Attribute> focus) {
        if (focus == null || focus.isEmpty()) {
            return develop(current, potential, age, supportFactor);
        }
        return spend(current, potential, focus, pointsFor(age, supportFactor));
    }

    /** The season's Development Points at an age, scaled by a support factor. */
    private static int pointsFor(int age, double supportFactor) {
        return (int) Math.round(DevelopmentPoints.award(CareerStage.of(age)) * Math.max(0.0, supportFactor));
    }

    /**
     * Applies a specific Development-Point allocation to attributes, bounded by {@code potential}: no
     * attribute is ever raised past its ceiling, so points aimed at a maxed-out attribute simply buy nothing.
     */
    public static Attributes applyAllocation(Attributes current, Attributes potential,
                                             Map<Attribute, Integer> allocation) {
        Attributes result = current;
        double totalGained = 0;
        for (Attribute a : Attribute.values()) { // deterministic order
            int points = allocation.getOrDefault(a, 0);
            int rating = result.get(a);
            int ceiling = ceilingFor(potential, a);
            while (points > 0 && rating < ceiling
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

    /**
     * Spends a single pool of points down a priority list: each attribute is raised toward its ceiling until
     * it is maxed or the season's cap is reached, and whatever is left carries on to the next. This is what
     * makes a focus worth setting — points that would be wasted on an attribute already at its ceiling roll
     * onward instead of vanishing.
     */
    private static Attributes spend(Attributes current, Attributes potential, List<Attribute> priority, int points) {
        Attributes result = current;
        double totalGained = 0;
        for (Attribute a : priority) {
            int rating = result.get(a);
            int ceiling = ceilingFor(potential, a);
            while (points > 0 && rating < ceiling
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

    /**
     * The Development Points the PLAYER banks for a completed season (spec: player-development): the base
     * player award scaled by career stage and a support/performance factor. The player spends these
     * themselves — they are not auto-allocated.
     */
    public static int playerSeasonAward(int age, double supportFactor) {
        return (int) Math.round(ProgressionConstants.PLAYER_DP_PER_SEASON
                * CareerStage.of(age).developmentMultiplier() * Math.max(0.0, supportFactor));
    }

    /**
     * The Development-Point cost of a set of attribute raises (each +1 costs more at higher ratings), clamped
     * so raises never exceed an attribute's ceiling — headroom past the ceiling costs nothing.
     */
    public static int costOf(Attributes current, Attributes potential, Map<Attribute, Integer> raises) {
        int total = 0;
        for (Map.Entry<Attribute, Integer> e : raises.entrySet()) {
            int rating = current.get(e.getKey());
            int ceiling = ceilingFor(potential, e.getKey());
            int steps = Math.max(0, e.getValue());
            for (int i = 0; i < steps && rating < ceiling; i++) {
                total += DevelopmentPoints.costToRaise(rating);
                rating++;
            }
        }
        return total;
    }

    /** Applies a set of attribute raises, each clamped to its ceiling (matches {@link #costOf}'s clamping). */
    public static Attributes applyRaises(Attributes current, Attributes potential, Map<Attribute, Integer> raises) {
        Attributes result = current;
        for (Map.Entry<Attribute, Integer> e : raises.entrySet()) {
            int rating = result.get(e.getKey());
            int target = Math.min(ceilingFor(potential, e.getKey()), rating + Math.max(0, e.getValue()));
            if (target != rating) {
                result = result.with(e.getKey(), target);
            }
        }
        return result;
    }

    /** An attribute's development ceiling: its potential, never above the numerical maximum. */
    private static int ceilingFor(Attributes potential, Attribute a) {
        return Math.min(potential.get(a), Attributes.MAX);
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
