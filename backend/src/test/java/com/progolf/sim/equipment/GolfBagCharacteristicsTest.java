package com.progolf.sim.equipment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** equipment-influence: the bag aggregates workability and feel (as well as forgiveness/power) into bonuses. */
class GolfBagCharacteristicsTest {

    private static GolfBag bagOf(EquipmentCharacteristics characteristics) {
        Map<EquipmentCategory, EquipmentItem> items = new EnumMap<>(EquipmentCategory.class);
        for (EquipmentCategory c : EquipmentCategory.values()) {
            items.put(c, new EquipmentItem(c + "-item", c, 0.5, characteristics, 0.0));
        }
        return new GolfBag(items);
    }

    @Test
    void standardGearGivesZeroBonuses() {
        GolfBag bag = bagOf(EquipmentCharacteristics.standard());
        assertThat(bag.workabilityBonus()).isZero();
        assertThat(bag.feelBonus()).isZero();
        assertThat(bag.forgivenessBonus()).isZero();
        assertThat(bag.powerBonus()).isZero();
    }

    @Test
    void upgradedGearGivesPositiveWorkabilityAndFeel() {
        GolfBag bag = bagOf(EquipmentCharacteristics.uniform(0.9)); // above the 0.5 baseline
        assertThat(bag.workabilityBonus()).isGreaterThan(0.0);
        assertThat(bag.feelBonus()).isGreaterThan(0.0);
    }
}
