package com.progolf.sim.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** tournament-loadout spec: loadout validity, bag derivation, only-owned integrity, neutral aggregates. */
class LoadoutBagTest {

    private static EquipmentInventory standardInventory() {
        EquipmentInventory inv = new EquipmentInventory();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            inv.add(EquipmentCatalogue.standardItem(category), 1, EquipmentAcquisition.Method.INITIAL);
        }
        return inv;
    }

    @Test
    void bestLoadoutFromInventoryIsValidAndComplete() {
        EquipmentInventory inv = standardInventory();
        TournamentLoadout loadout = TournamentLoadout.bestFrom(inv);
        assertThat(loadout.selection()).hasSize(EquipmentCategory.values().length);
        assertThat(loadout.isValid(inv)).isTrue();
    }

    @Test
    void aLoadoutWithUnownedEquipmentIsInvalid() {
        EquipmentInventory inv = standardInventory();
        Map<EquipmentCategory, EquipmentItem> selection = new EnumMap<>(TournamentLoadout.bestFrom(inv).selection());
        selection.put(EquipmentCategory.PUTTER, new EquipmentItem("Unowned Putter", EquipmentCategory.PUTTER,
                EquipmentBrand.MERIDIAN, 0.9, EquipmentCharacteristics.uniform(0.9), 40_000)); // not in the inventory
        TournamentLoadout invalid = new TournamentLoadout(selection);
        assertThat(invalid.isValid(inv)).isFalse();
    }

    @Test
    void standardBagIsCompleteAndExactlyNeutral() {
        GolfBag bag = GolfBag.fromLoadout(TournamentLoadout.bestFrom(standardInventory()));
        assertThat(bag.isValid()).isTrue();
        assertThat(bag.forgivenessBonus()).isCloseTo(0.0, within(1e-9));
        assertThat(bag.powerBonus()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void strongerEquipmentRaisesTheBagBonuses() {
        EquipmentInventory inv = standardInventory();
        EquipmentItem proDriver = new EquipmentItem("Pro Driver", EquipmentCategory.DRIVER, EquipmentBrand.MERIDIAN,
                0.95, EquipmentCharacteristics.uniform(0.95), 57_000);
        inv.add(proDriver, 2, EquipmentAcquisition.Method.PURCHASE);
        GolfBag bag = GolfBag.fromLoadout(TournamentLoadout.bestFrom(inv));
        assertThat(bag.forgivenessBonus()).isGreaterThan(0.0);
        assertThat(bag.powerBonus()).isGreaterThan(0.0);
    }
}
