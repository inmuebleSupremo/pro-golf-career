package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.equipment.EquipmentDeal;
import com.progolf.sim.equipment.EquipmentItem;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import org.junit.jupiter.api.Test;

/** equipment-influence (brand deals): a deal pays the player, kits their bag, and locks them in for the term. */
class WorldEquipmentDealTest {

    private static World playerWorld(long seed) {
        World world = World.create(seed, WorldConfig.defaults());
        world.createPlayer("Deal", "Maker", Nationality.USA, 20, Archetype.ALL_ROUNDER);
        return world;
    }

    private static String playerId(World world) {
        return world.playerGolferId().orElseThrow();
    }

    @Test
    void signingADealPaysKitsTheBagAndLocksIn() {
        World world = playerWorld(31L);
        String id = playerId(world);
        world.advanceSeason(); // free agent → deal offers generated for the upcoming season

        assertThat(world.pendingEquipmentDeals()).as("a free agent is offered brand deals").isNotEmpty();
        assertThat(world.activeEquipmentDeal()).isNull();

        EquipmentDeal deal = world.pendingEquipmentDeals().get(0);
        double before = world.financialAccountOf(id).availableFunds();
        world.acceptEquipmentDeal(0);

        // Signing bonus was paid, the deal is now active, and offers are withdrawn (locked in).
        assertThat(world.financialAccountOf(id).availableFunds()).isEqualTo(before + deal.signingBonus());
        assertThat(world.activeEquipmentDeal()).isEqualTo(deal);
        assertThat(world.pendingEquipmentDeals()).isEmpty();

        // The whole loadout is now the brand's gear.
        assertThat(world.tournamentLoadoutOf(id).selection().values())
                .allSatisfy(item -> assertThat(item.brand()).isEqualTo(deal.brand()))
                .hasSize(7);
    }

    @Test
    void aDealPaysARetainerEachSeasonThenExpiresToFreeAgency() {
        World world = playerWorld(32L);
        String id = playerId(world);
        world.advanceSeason();
        EquipmentDeal deal = world.pendingEquipmentDeals().get(0);
        world.acceptEquipmentDeal(0);

        // Advance through the deal's term: each active season pays the retainer (as commercial income, so this
        // is immune to the season's competing expenses) and keeps the player locked.
        double incomeBefore = world.financialAccountOf(id).snapshot().sponsorshipIncome();
        world.advanceSeason();
        assertThat(world.financialAccountOf(id).snapshot().sponsorshipIncome())
                .as("retainer paid").isGreaterThanOrEqualTo(incomeBefore + deal.perSeasonRetainer());
        if (world.activeEquipmentDeal() != null) {
            assertThat(world.pendingEquipmentDeals()).as("no new deals while locked in").isEmpty();
        }

        // Advance until the deal ends; the player becomes a free agent and is offered deals again.
        for (int i = 0; i < deal.durationSeasons() + 1 && world.activeEquipmentDeal() != null; i++) {
            world.advanceSeason();
        }
        assertThat(world.activeEquipmentDeal()).as("deal expired → free agent").isNull();
        assertThat(world.pendingEquipmentDeals()).as("free agent offered deals again").isNotEmpty();
    }

    @Test
    void everyDealOfferProvidesQualityGear() {
        World world = playerWorld(33L);
        world.advanceSeason();
        for (EquipmentDeal deal : world.pendingEquipmentDeals()) {
            assertThat(deal.gearTier()).isGreaterThan(0.6);
            assertThat(deal.perSeasonRetainer()).isGreaterThan(0.0);
            assertThat(deal.durationSeasons()).isBetween(2, 4);
        }
        // Offers come from distinct brands.
        assertThat(world.pendingEquipmentDeals().stream().map(EquipmentDeal::brand).distinct().count())
                .isEqualTo(world.pendingEquipmentDeals().size());
    }
}
