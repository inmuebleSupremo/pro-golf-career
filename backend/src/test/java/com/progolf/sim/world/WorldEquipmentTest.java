package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.economy.Transaction;
import com.progolf.sim.economy.TransactionType;
import com.progolf.sim.equipment.EquipmentAcquisition;
import com.progolf.sim.equipment.EquipmentCategory;
import com.progolf.sim.equipment.EquipmentInventory;
import java.util.List;
import org.junit.jupiter.api.Test;

/** world-progression (modified): standard kit, Economy-integrated acquisition, applied bag; reproducible. */
class WorldEquipmentTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void everyGolferStartsWithFullStandardKit() {
        World world = World.create(101L, small());
        for (String id : world.activeGolferIds()) {
            EquipmentInventory inv = world.equipmentInventoryOf(id);
            assertThat(inv).isNotNull();
            assertThat(inv.all()).hasSize(EquipmentCategory.values().length);
            assertThat(world.tournamentLoadoutOf(id).isValid(inv)).isTrue();
        }
    }

    @Test
    void golfersAcquireEquipmentPaidThroughTheEconomy() {
        World world = World.create(202L, small());
        world.advanceSeason();
        world.advanceSeason();
        world.advanceSeason();

        boolean anyUpgraded = world.activeGolferIds().stream()
                .map(world::equipmentInventoryOf)
                .anyMatch(inv -> inv.all().size() > EquipmentCategory.values().length);
        assertThat(anyUpgraded).as("some golfer purchased an upgrade").isTrue();

        boolean anyPurchaseCharge = world.activeGolferIds().stream()
                .map(world::financialAccountOf)
                .flatMap(a -> a.ledger().stream())
                .map(Transaction::type)
                .anyMatch(t -> t == TransactionType.EQUIPMENT_PURCHASE);
        assertThat(anyPurchaseCharge).as("equipment is charged through the account").isTrue();
    }

    @Test
    void ownershipHistoryIsPreserved() {
        World world = World.create(303L, small());
        world.advanceSeason();
        world.advanceSeason();

        boolean anyPurchaseInHistory = world.activeGolferIds().stream()
                .map(world::equipmentInventoryOf)
                .flatMap(inv -> inv.history().stream())
                .anyMatch(a -> a.method() == EquipmentAcquisition.Method.PURCHASE);
        assertThat(anyPurchaseInHistory).isTrue();
    }

    @Test
    void equipmentStateIsReproducibleFromTheSeed() {
        World a = World.create(404L, small());
        World b = World.create(404L, small());
        a.advanceSeason();
        a.advanceSeason();
        b.advanceSeason();
        b.advanceSeason();

        assertThat(summary(a)).isEqualTo(summary(b));
    }

    private static List<String> summary(World w) {
        return w.activeGolferIds().stream().sorted()
                .map(id -> {
                    EquipmentInventory inv = w.equipmentInventoryOf(id);
                    double qualitySum = inv.all().stream().mapToDouble(i -> i.quality()).sum();
                    return id + "|" + inv.all().size() + "|" + inv.history().size() + "|" + qualitySum;
                })
                .toList();
    }
}
