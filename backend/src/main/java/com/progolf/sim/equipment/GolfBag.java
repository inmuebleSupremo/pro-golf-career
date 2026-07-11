package com.progolf.sim.equipment;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The active collection of equipment carried during play (spec: tournament-loadout, REQ-206), derived
 * from a {@link TournamentLoadout}. The shot engine consumes the bag's aggregate characteristics — never
 * the full inventory. The aggregate {@link #forgivenessBonus()} / {@link #powerBonus()} are zero at the
 * baseline (standard gear) and positive for stronger gear, so standard equipment is exactly neutral.
 */
public record GolfBag(Map<EquipmentCategory, EquipmentItem> items) {

    public GolfBag {
        Objects.requireNonNull(items, "items");
        items = new EnumMap<>(items);
    }

    /** Derives the active bag from a prepared loadout. */
    public static GolfBag fromLoadout(TournamentLoadout loadout) {
        return new GolfBag(loadout.selection());
    }

    /** Whether the bag covers every category (a valid, complete bag). */
    public boolean isValid() {
        return items.size() == EquipmentCategory.values().length;
    }

    /** Above-baseline mean forgiveness scaled into a dispersion-reduction bonus (0 at baseline). */
    public double forgivenessBonus() {
        return aboveBaseline(meanForgiveness()) * EquipmentConstants.FORGIVENESS_SCALE;
    }

    /** Above-baseline mean power scaled into a reach-extension bonus (0 at baseline). */
    public double powerBonus() {
        return aboveBaseline(meanPower()) * EquipmentConstants.POWER_SCALE;
    }

    /** Above-baseline mean workability scaled into a wind-control bonus (0 at baseline). */
    public double workabilityBonus() {
        return aboveBaseline(meanOf(EquipmentCharacteristics::workability)) * EquipmentConstants.WORKABILITY_SCALE;
    }

    /** Above-baseline mean feel scaled into a distance-control bonus (0 at baseline). */
    public double feelBonus() {
        return aboveBaseline(meanOf(EquipmentCharacteristics::feel)) * EquipmentConstants.FEEL_SCALE;
    }

    private double meanOf(java.util.function.ToDoubleFunction<EquipmentCharacteristics> characteristic) {
        double sum = 0;
        for (EquipmentItem i : items.values()) {
            sum += characteristic.applyAsDouble(i.characteristics());
        }
        return items.isEmpty() ? EquipmentConstants.BASELINE_CHARACTERISTIC : sum / items.size();
    }

    private double meanForgiveness() {
        double sum = 0;
        for (EquipmentItem i : items.values()) {
            sum += i.characteristics().forgiveness();
        }
        return items.isEmpty() ? EquipmentConstants.BASELINE_CHARACTERISTIC : sum / items.size();
    }

    private double meanPower() {
        double sum = 0;
        for (EquipmentItem i : items.values()) {
            sum += i.characteristics().power();
        }
        return items.isEmpty() ? EquipmentConstants.BASELINE_CHARACTERISTIC : sum / items.size();
    }

    private static double aboveBaseline(double value) {
        return Math.max(0.0, value - EquipmentConstants.BASELINE_CHARACTERISTIC);
    }
}
