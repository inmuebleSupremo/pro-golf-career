package com.progolf.sim.equipment;

import com.progolf.sim.core.Rng;
import java.util.Optional;

/**
 * The deterministic policy by which a golfer decides an equipment upgrade (spec: equipment-inventory,
 * REQ-210). It targets the weakest category (the lowest current best quality), generates a candidate, and
 * proposes it only if it improves on what is owned — at most one upgrade per season (gradual). Affordability
 * is checked separately by the caller. The same rules apply to every golfer, so decisions are reproducible.
 */
public final class AcquisitionPolicy {

    private AcquisitionPolicy() {
    }

    /** An upgrade worth acquiring this season, or empty if nothing owned would be improved. */
    public static Optional<EquipmentItem> chooseUpgrade(EquipmentInventory inventory, Rng rng) {
        EquipmentCategory weakest = null;
        double weakestQuality = Double.MAX_VALUE;
        for (EquipmentCategory category : EquipmentCategory.values()) {
            double best = inventory.bestIn(category).map(EquipmentItem::quality).orElse(0.0);
            if (best < weakestQuality) {
                weakestQuality = best;
                weakest = category;
            }
        }
        if (weakest == null) {
            return Optional.empty();
        }
        EquipmentItem candidate = EquipmentCatalogue.generateUpgrade(weakest, rng);
        return candidate.quality() > weakestQuality ? Optional.of(candidate) : Optional.empty();
    }

    /** Whether the golfer can afford the item. */
    public static boolean canAfford(double availableFunds, EquipmentItem item) {
        return availableFunds >= item.cost();
    }
}
