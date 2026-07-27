package com.progolf.sim.equipment;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;

/**
 * How well a piece of gear's <i>shape</i> fits a golfer's build (spec: equipment-influence). Each of the four
 * characteristics shores up a different weakness — forgiveness helps a wayward driver/iron player (tightens
 * dispersion), power helps a short hitter (extends reach), workability helps in wind (course management),
 * and feel helps distance control/proximity. Fit measures whether an item concentrates its strength where
 * the golfer most needs help, <i>independent of the item's overall quality</i> (a balanced item is a neutral
 * 0.5 fit at any tier; a well-matched trade-off scores higher, a mismatched one lower).
 *
 * <p>Pure and deterministic — a UI/decision signal, not a shot-engine input (the engine already consumes the
 * characteristics directly, so a good fit pays off in play on its own).
 */
public final class EquipmentFit {

    private EquipmentFit() {
    }

    /** How pronounced a shape match/mismatch moves fit away from the neutral 0.5. */
    private static final double FIT_SENSITIVITY = 1.5;

    /**
     * The fit in [0,1] of {@code gear} to {@code build}: 0.5 is neutral (a balanced item, or a golfer with no
     * particular weakness), above 0.5 means the gear is strong where the golfer is weak, below means it
     * emphasises what the golfer already does well.
     */
    public static double fit(EquipmentCharacteristics gear, Attributes build) {
        double needForgiveness = weakness(build, Attribute.DRIVING_ACCURACY, Attribute.IRONS_ACCURACY);
        double needPower = weakness(build, Attribute.DRIVING_DISTANCE);
        double needWorkability = weakness(build, Attribute.COURSE_MANAGEMENT);
        double needFeel = weakness(build, Attribute.PUTTING_PROXIMITY, Attribute.IRONS_CONTROL);
        double totalNeed = needForgiveness + needPower + needWorkability + needFeel;
        if (totalNeed <= 0) {
            return 0.5; // no weaknesses to complement — every shape is neutral
        }

        double mean = (gear.forgiveness() + gear.power() + gear.workability() + gear.feel()) / 4.0;
        // Need-weighted sum of how the item redistributes around its own mean: positive when its strengths
        // land on the golfer's weaknesses. Needs are normalised to a distribution so the scale is stable.
        double alignment = (needForgiveness * (gear.forgiveness() - mean)
                + needPower * (gear.power() - mean)
                + needWorkability * (gear.workability() - mean)
                + needFeel * (gear.feel() - mean)) / totalNeed;
        return clampUnit(0.5 + FIT_SENSITIVITY * alignment);
    }

    /** The fit of a whole prepared bag to a build — the mean fit of its items. */
    public static double bagFit(GolfBag bag, Attributes build) {
        if (bag.items().isEmpty()) {
            return 0.5;
        }
        double sum = 0;
        for (EquipmentItem item : bag.items().values()) {
            sum += fit(item.characteristics(), build);
        }
        return sum / bag.items().size();
    }

    /** How weak the golfer is across the given attributes (0 = elite, 1 = the floor) — the need for help. */
    private static double weakness(Attributes build, Attribute... attributes) {
        double sum = 0;
        for (Attribute a : attributes) {
            sum += build.norm(a);
        }
        return 1.0 - sum / attributes.length;
    }

    private static double clampUnit(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }
}
