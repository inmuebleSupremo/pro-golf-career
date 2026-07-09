package com.progolf.sim.equipment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** equipment-inventory spec: ownership, categories, acquisition history. */
class EquipmentInventoryTest {

    private static EquipmentInventory standardInventory() {
        EquipmentInventory inv = new EquipmentInventory();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            inv.add(EquipmentCatalogue.standardItem(category), 1, EquipmentAcquisition.Method.INITIAL);
        }
        return inv;
    }

    @Test
    void startingInventoryOwnsOneItemPerCategoryWithRecordedOwnership() {
        EquipmentInventory inv = standardInventory();
        assertThat(inv.all()).hasSize(EquipmentCategory.values().length);
        for (EquipmentCategory category : EquipmentCategory.values()) {
            assertThat(inv.bestIn(category)).isPresent();
        }
        assertThat(inv.history()).hasSize(EquipmentCategory.values().length)
                .allSatisfy(a -> assertThat(a.method()).isEqualTo(EquipmentAcquisition.Method.INITIAL));
    }

    @Test
    void acquisitionAddsItemsAndRecordsHistory() {
        EquipmentInventory inv = standardInventory();
        EquipmentItem proDriver = new EquipmentItem("Pro Driver", EquipmentCategory.DRIVER, 0.9,
                EquipmentCharacteristics.uniform(0.9), 55_000);
        inv.add(proDriver, 3, EquipmentAcquisition.Method.PURCHASE);

        assertThat(inv.owns(proDriver)).isTrue();
        assertThat(inv.itemsIn(EquipmentCategory.DRIVER)).hasSize(2);
        assertThat(inv.bestIn(EquipmentCategory.DRIVER)).hasValue(proDriver); // higher quality wins
        assertThat(inv.history()).last().satisfies(a -> {
            assertThat(a.method()).isEqualTo(EquipmentAcquisition.Method.PURCHASE);
            assertThat(a.season()).isEqualTo(3);
            assertThat(a.category()).isEqualTo(EquipmentCategory.DRIVER);
        });
    }
}
