package com.progolf.sim.equipment;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SplitMix64Rng;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** equipment-inventory spec: deterministic catalogue + acquisition policy (REQ-210). */
class AcquisitionTest {

    private static EquipmentInventory standardInventory() {
        EquipmentInventory inv = new EquipmentInventory();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            inv.add(EquipmentCatalogue.standardItem(category), 1, EquipmentAcquisition.Method.INITIAL);
        }
        return inv;
    }

    @Test
    void standardItemIsBaselineAndFree() {
        EquipmentItem standard = EquipmentCatalogue.standardItem(EquipmentCategory.IRONS);
        assertThat(standard.quality()).isEqualTo(EquipmentConstants.BASELINE_CHARACTERISTIC);
        assertThat(standard.cost()).isZero();
    }

    @Test
    void upgradesAreStrongerAndCostlyAndDeterministic() {
        EquipmentItem a = EquipmentCatalogue.generateUpgrade(EquipmentCategory.DRIVER, new SplitMix64Rng(5L));
        EquipmentItem b = EquipmentCatalogue.generateUpgrade(EquipmentCategory.DRIVER, new SplitMix64Rng(5L));
        assertThat(a).isEqualTo(b); // deterministic
        assertThat(a.quality()).isGreaterThan(EquipmentConstants.BASELINE_CHARACTERISTIC);
        assertThat(a.cost()).isGreaterThan(0.0);
    }

    @Test
    void policyTargetsTheWeakestCategoryAndProposesAnImprovement() {
        EquipmentInventory inv = standardInventory();
        Optional<EquipmentItem> upgrade = AcquisitionPolicy.chooseUpgrade(inv, new SplitMix64Rng(1L));
        assertThat(upgrade).isPresent();
        assertThat(upgrade.get().quality()).isGreaterThan(EquipmentConstants.BASELINE_CHARACTERISTIC);
    }

    @Test
    void affordabilityGatesOnCost() {
        EquipmentItem item = EquipmentCatalogue.generateUpgrade(EquipmentCategory.IRONS, new SplitMix64Rng(2L));
        assertThat(AcquisitionPolicy.canAfford(item.cost() + 1, item)).isTrue();
        assertThat(AcquisitionPolicy.canAfford(item.cost() - 1, item)).isFalse();
    }
}
