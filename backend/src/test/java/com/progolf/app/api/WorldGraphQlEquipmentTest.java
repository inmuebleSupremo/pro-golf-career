package com.progolf.app.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.api.dto.EquipmentItemDto;
import com.progolf.app.world.WorldService;
import com.progolf.app.world.WorldSession;
import com.progolf.sim.equipment.EquipmentAcquisition;
import com.progolf.sim.equipment.EquipmentBrand;
import com.progolf.sim.equipment.EquipmentCategory;
import com.progolf.sim.equipment.EquipmentCharacteristics;
import com.progolf.sim.equipment.EquipmentItem;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.world.WorldConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * graphql-api (add-graphql-equipment-loadout): owned-equipment reads and the usable loadout-selection
 * mutation (addressing an owned item by category + name). Preconditions are arranged via {@link WorldService}
 * under the same authenticated owner ({@link #OWNER}) the resolvers scope to (spec: resource-ownership).
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@WithMockUser(username = WorldGraphQlEquipmentTest.OWNER)
class WorldGraphQlEquipmentTest {

    static final String OWNER = "owner-equipment-test";

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);
    private static final String LOADOUT_FIELDS =
            "{ name category brand quality cost forgiveness power workability feel fit }";

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private WorldService worldService;

    private String createWorldWithPlayer(long seed) {
        WorldSession session = worldService.create(OWNER, seed, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Gear", "Head", Nationality.USA, 20, Archetype.ALL_ROUNDER);
        return session.id();
    }

    @Test
    void ownedEquipmentAndLoadoutAreReadable() {
        String id = createWorldWithPlayer(2001L);

        // A created player is admitted with the standard kit: one item selected in every category.
        graphQlTester.document("query($id: ID!){ playerLoadout(id: $id) " + LOADOUT_FIELDS + " }")
                .variable("id", id).execute()
                .path("playerLoadout").entityList(EquipmentItemDto.class)
                .satisfies(loadout -> assertThat(loadout).hasSize(EquipmentCategory.values().length));

        graphQlTester.document("query($id: ID!){ playerEquipment(id: $id){ name category } }")
                .variable("id", id).execute()
                .path("playerEquipment").entityList(Object.class)
                .satisfies(owned -> assertThat(owned).isNotEmpty());
    }

    @Test
    void readsAreEmptyWithoutAPlayer() {
        String id = worldService.create(OWNER, 2002L, SMALL).id();
        graphQlTester.document("""
                        query($id: ID!){
                          playerEquipment(id: $id){ name }
                          playerLoadout(id: $id){ name }
                        }
                        """)
                .variable("id", id).execute()
                .path("playerEquipment").entityList(Object.class).hasSize(0)
                .path("playerLoadout").entityList(Object.class).hasSize(0);
    }

    @Test
    void loadoutCanBeSwappedToAnOwnedItem() {
        WorldSession session = worldService.create(OWNER, 2003L, SMALL);
        String golferId = worldService.createPlayer(OWNER, session.id(), "Gear", "Head", Nationality.USA, 20,
                Archetype.ALL_ROUNDER);

        // Arrange a second owned DRIVER (an upgrade) directly on the player's inventory.
        EquipmentItem upgrade = new EquipmentItem("Apex Driver 777", EquipmentCategory.DRIVER, EquipmentBrand.APEX,
                0.9, EquipmentCharacteristics.uniform(0.9), 100.0);
        session.world().equipmentInventoryOf(golferId)
                .add(upgrade, session.world().currentSeason(), EquipmentAcquisition.Method.PURCHASE);

        graphQlTester.document("""
                        mutation($id: ID!){ selectLoadoutItem(id: $id, category: "DRIVER", name: "Apex Driver 777") }
                        """)
                .variable("id", session.id()).execute()
                .path("selectLoadoutItem").entity(Boolean.class).isEqualTo(true);

        // The DRIVER slot of the loadout now holds the upgrade.
        graphQlTester.document("query($id: ID!){ playerLoadout(id: $id) " + LOADOUT_FIELDS + " }")
                .variable("id", session.id()).execute()
                .path("playerLoadout").entityList(EquipmentItemDto.class)
                .satisfies(loadout -> assertThat(loadout)
                        .filteredOn(i -> i.category().equals("DRIVER"))
                        .singleElement()
                        .extracting(EquipmentItemDto::name)
                        .isEqualTo("Apex Driver 777"));
    }

    @Test
    void aBrandDealCanBeReadAndSigned() {
        WorldSession session = worldService.create(OWNER, 2005L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Free", "Agent", Nationality.USA, 20, Archetype.ALL_ROUNDER);
        worldService.advanceSeason(OWNER, session.id()); // free agent → brand-deal offers appear

        // A free agent is offered brand deals with terms + a fit score, and has no active deal yet.
        graphQlTester.document("""
                        query($id: ID!){
                          pendingEquipmentDeals(id: $id){ brand perSeasonRetainer signingBonus durationSeasons gearTier gearFit }
                          activeEquipmentDeal(id: $id){ brand }
                        }
                        """)
                .variable("id", session.id()).execute()
                .path("pendingEquipmentDeals").entityList(Object.class)
                .satisfies(deals -> assertThat(deals).isNotEmpty())
                .path("pendingEquipmentDeals[0].brand").entity(String.class).satisfies(b -> assertThat(b).isNotBlank())
                .path("pendingEquipmentDeals[0].gearTier").entity(Double.class).satisfies(t -> assertThat(t).isGreaterThan(0.6))
                .path("activeEquipmentDeal").valueIsNull();

        // Sign the first offer; it becomes the active deal and the offers are withdrawn.
        graphQlTester.document("mutation($id: ID!){ acceptEquipmentDeal(id: $id, index: 0) }")
                .variable("id", session.id()).execute()
                .path("acceptEquipmentDeal").entity(Boolean.class).isEqualTo(true);

        graphQlTester.document("""
                        query($id: ID!){
                          activeEquipmentDeal(id: $id){ brand seasonsRemaining }
                          pendingEquipmentDeals(id: $id){ brand }
                        }
                        """)
                .variable("id", session.id()).execute()
                .path("activeEquipmentDeal.seasonsRemaining").entity(Integer.class)
                .satisfies(r -> assertThat(r).isPositive())
                .path("pendingEquipmentDeals").entityList(Object.class).hasSize(0);
    }

    @Test
    void selectingAnUnownedItemIsBadRequest() {
        String id = createWorldWithPlayer(2004L);
        graphQlTester.document("""
                        mutation($id: ID!){ selectLoadoutItem(id: $id, category: "DRIVER", name: "ghost-club") }
                        """)
                .variable("id", id).execute().errors()
                .expect(error -> error.getErrorType() == ErrorType.BAD_REQUEST);
    }
}
