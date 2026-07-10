package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.equipment.EquipmentItem;
import com.progolf.sim.staff.StaffMember;
import org.junit.jupiter.api.Test;

/** player-control (modified): the player manages their own staff and equipment via pending offers. */
class WorldPlayerManagementTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    private static World assignedWorld(long seed) {
        World world = World.create(seed, small());
        world.assignPlayer(world.activeGolferIds().get(0));
        world.advanceSeason(); // season transition generates the player's pending staff & equipment offers
        return world;
    }

    @Test
    void hiringAStaffCandidateAddsThemAndChargesTheAccount() {
        World world = assignedWorld(1L);
        String id = world.playerGolferId().orElseThrow();
        assertThat(world.supportTeamOf(id).size()).isZero();

        StaffMember candidate = world.pendingStaffOffers().get(0);
        double fundsBefore = world.financialAccountOf(id).availableFunds();
        int offersBefore = world.pendingStaffOffers().size();

        world.hireStaff(0);

        assertThat(world.supportTeamOf(id).has(candidate.role())).isTrue();
        assertThat(world.pendingStaffOffers()).hasSize(offersBefore - 1);
        assertThat(world.financialAccountOf(id).availableFunds())
                .isEqualTo(fundsBefore - candidate.hiringCost());
    }

    @Test
    void releasingAStaffMemberVacatesTheRoleAndRecordsHistory() {
        World world = assignedWorld(2L);
        String id = world.playerGolferId().orElseThrow();
        StaffMember candidate = world.pendingStaffOffers().get(0);
        world.hireStaff(0);
        assertThat(world.supportTeamOf(id).has(candidate.role())).isTrue();

        world.releaseStaff(candidate.role());

        assertThat(world.supportTeamOf(id).has(candidate.role())).isFalse();
        // History is preserved (the relationship is closed, not deleted) — no active relationship remains.
        assertThat(world.supportTeamOf(id).history()).isNotEmpty();
        assertThat(world.supportTeamOf(id).history())
                .noneMatch(r -> r.role() == candidate.role() && r.isActive());
    }

    @Test
    void buyingAnUpgradeAddsItChargesAndMakesItSelectable() {
        World world = assignedWorld(3L);
        String id = world.playerGolferId().orElseThrow();
        EquipmentItem upgrade = world.pendingEquipmentOffers().get(0);
        double fundsBefore = world.financialAccountOf(id).availableFunds();
        int ownedBefore = world.equipmentInventoryOf(id).all().size();

        world.buyEquipment(0);

        assertThat(world.equipmentInventoryOf(id).owns(upgrade)).isTrue();
        assertThat(world.equipmentInventoryOf(id).all()).hasSize(ownedBefore + 1);
        assertThat(world.financialAccountOf(id).availableFunds()).isEqualTo(fundsBefore - upgrade.cost());
        assertThat(world.tournamentLoadoutOf(id).selection().get(upgrade.category())).isEqualTo(upgrade);
    }

    @Test
    void selectingAnOwnedItemSetsTheLoadout() {
        World world = assignedWorld(4L);
        String id = world.playerGolferId().orElseThrow();
        EquipmentItem upgrade = world.pendingEquipmentOffers().get(0);
        var category = upgrade.category();
        EquipmentItem standard = world.tournamentLoadoutOf(id).selection().get(category);

        world.buyEquipment(0); // the loadout now uses the upgrade for that category
        assertThat(world.tournamentLoadoutOf(id).selection().get(category)).isEqualTo(upgrade);

        // The player re-selects the standard item — they control the bag from owned gear.
        world.selectLoadoutItem(standard);
        assertThat(world.tournamentLoadoutOf(id).selection().get(category)).isEqualTo(standard);
    }

    @Test
    void anUnaffordableHireIsANoOp() {
        World world = assignedWorld(5L);
        String id = world.playerGolferId().orElseThrow();
        // Drain the account so nothing is affordable.
        double funds = world.financialAccountOf(id).availableFunds();
        if (funds > 0) {
            world.financialAccountOf(id).charge(com.progolf.sim.economy.TransactionType.TRAVEL, funds,
                    java.time.LocalDate.now(), "drain");
        }
        int offersBefore = world.pendingStaffOffers().size();
        double fundsBefore = world.financialAccountOf(id).availableFunds();

        world.hireStaff(0);

        assertThat(world.supportTeamOf(id).size()).isZero(); // nothing hired
        assertThat(world.pendingStaffOffers()).hasSize(offersBefore); // offer remains
        assertThat(world.financialAccountOf(id).availableFunds()).isEqualTo(fundsBefore); // no charge
    }

    @Test
    void anUnassignedWorldIsByteIdenticalAcrossSeasons() {
        // The AI staff/equipment paths are untouched: two same-seed worlds with no player stay identical.
        World a = World.create(99L, small());
        World b = World.create(99L, small());
        for (int i = 0; i < 3; i++) {
            a.advanceSeason();
            b.advanceSeason();
        }
        assertThat(a.currentRanking()).isEqualTo(b.currentRanking());
        assertThat(a.newsFeed()).isEqualTo(b.newsFeed());
        assertThat(a.records()).isEqualTo(b.records());
    }
}
