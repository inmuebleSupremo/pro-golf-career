package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;

/**
 * How much of their potential a golfer has realised at a given age (spec: player-development).
 *
 * <p>This is the counterpart to {@link ProgressionEngine#develop}: development walks a golfer toward their
 * ceiling one season at a time, and this curve says where along that walk a golfer of a given age should
 * already be. It exists so a world can be seeded with golfers of every age — a 17-year-old rookie a long way
 * from their ceiling, a 30-year-old at it, a 44-year-old veteran past their physical peak — instead of one
 * cohort of teenagers that ages in lockstep with no one coming through behind them.
 *
 * <p>Maturity rises linearly from {@link ProgressionConstants#MATURITY_AT_ENTRY} at the entry age to a fully
 * realised 1.0 at {@link ProgressionConstants#MATURITY_AGE}, and stays there: past that age a golfer's
 * ability is governed by aging, not by how much headroom they had left.
 */
public final class Maturity {

    private Maturity() {
    }

    /** The fraction of their potential a golfer of this age is expected to have realised, in [0,1]. */
    public static double at(int age) {
        int entry = ProgressionConstants.MATURITY_ENTRY_AGE;
        int mature = ProgressionConstants.MATURITY_AGE;
        if (age >= mature) {
            return 1.0;
        }
        if (age <= entry) {
            return ProgressionConstants.MATURITY_AT_ENTRY;
        }
        double progress = (double) (age - entry) / (mature - entry);
        return ProgressionConstants.MATURITY_AT_ENTRY
                + (1.0 - ProgressionConstants.MATURITY_AT_ENTRY) * progress;
    }

    /**
     * The attributes a golfer of {@code age} with this {@code potential} starts with: their ceiling scaled
     * back to the share of it they have realised so far. Never exceeds potential.
     */
    public static Attributes abilityAt(Attributes potential, int age) {
        double maturity = at(age);
        java.util.Map<Attribute, Integer> values = new java.util.EnumMap<>(Attribute.class);
        for (Attribute a : Attribute.values()) {
            values.put(a, Attributes.clamp((int) Math.round(potential.get(a) * maturity)));
        }
        return Attributes.of(values);
    }
}
