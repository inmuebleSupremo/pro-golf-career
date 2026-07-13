package com.progolf.app.api;

import com.progolf.app.world.WorldService;
import com.progolf.app.world.WorldSession;
import com.progolf.sim.control.CareerGoal;
import com.progolf.sim.control.GoalType;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.world.WorldConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * graphql-api: the read model and world-lifecycle mutations are served over GraphQL. Read state that depends
 * on the not-yet-exposed mutations (an assigned player, chosen goals) is arranged by injecting
 * {@link WorldService} directly (design D7). Runs as an authenticated user ({@link #OWNER}); the resolvers
 * scope to that owner (spec: resource-ownership), which is also the owner the arranged sessions are created
 * under, so the two match.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@WithMockUser(username = WorldGraphQlApiTest.OWNER)
class WorldGraphQlApiTest {

    static final String OWNER = "owner-api-test";

    /** A small world so tests that advance stay fast (default prestige counts via the 5-arg ctor). */
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private WorldService worldService;

    // --- Lifecycle mutations ---

    @Test
    void createWorldWithoutConfigReturnsInitialStatus() {
        graphQlTester.document("""
                        mutation { createWorld(seed: 42) { id season week activePopulation hasPendingEvent } }
                        """)
                .execute()
                .path("createWorld.season").entity(Integer.class).isEqualTo(1)
                .path("createWorld.hasPendingEvent").entity(Boolean.class).isEqualTo(false)
                .path("createWorld.activePopulation").entity(Integer.class).satisfies(p -> assertPositive(p))
                .path("createWorld.id").entity(String.class).satisfies(id -> assertNonBlank(id));
    }

    @Test
    void createWorldWithConfigHonorsIt() {
        // A population of 40 proves the config was applied (a default world has hundreds active).
        graphQlTester.document("""
                        mutation Create($cfg: WorldConfigInput) {
                          createWorld(seed: 7, config: $cfg) { activePopulation season }
                        }
                        """)
                .variable("cfg", java.util.Map.of(
                        "populationSize", 40, "weeksPerSeason", 6, "eventsPerTierPerSeason", 3,
                        "fieldSize", 20, "coursePoolSize", 4))
                .execute()
                .path("createWorld.season").entity(Integer.class).isEqualTo(1)
                .path("createWorld.activePopulation").entity(Integer.class)
                .satisfies(p -> org.assertj.core.api.Assertions.assertThat(p).isPositive().isLessThanOrEqualTo(40));
    }

    @Test
    void advanceWeekAndSeasonAdvanceStatus() {
        String id = worldService.create(OWNER, 101L, SMALL).id();

        graphQlTester.document("mutation($id: ID!){ advanceWeek(id: $id){ week season } }")
                .variable("id", id).execute()
                .path("advanceWeek.week").entity(Integer.class).isEqualTo(2);

        graphQlTester.document("mutation($id: ID!){ advanceSeason(id: $id){ season } }")
                .variable("id", id).execute()
                .path("advanceSeason.season").entity(Integer.class).isEqualTo(2);
    }

    // --- Read model ---

    @Test
    void worldQueryReturnsStatus() {
        String id = worldService.create(OWNER, 202L, SMALL).id();
        graphQlTester.document("query($id: ID!){ world(id: $id){ id season week activePopulation hasPendingEvent } }")
                .variable("id", id).execute()
                .path("world.id").entity(String.class).isEqualTo(id)
                .path("world.season").entity(Integer.class).isEqualTo(1)
                .path("world.hasPendingEvent").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void readModelIsEmptyForAWorldWithoutAPlayer() {
        String id = worldService.create(OWNER, 303L, SMALL).id();
        graphQlTester.document("""
                        query($id: ID!){
                          playerSchedule(id: $id){ tournamentId }
                          careerGoals(id: $id){ type }
                          pendingSponsorships(id: $id){ sponsor }
                          hallOfFame(id: $id){ golferId }
                        }
                        """)
                .variable("id", id).execute()
                .path("playerSchedule").entityList(Object.class).hasSize(0)
                .path("careerGoals").entityList(Object.class).hasSize(0)
                .path("pendingSponsorships").entityList(Object.class).hasSize(0)
                .path("hallOfFame").entityList(Object.class).hasSize(0);
    }

    @Test
    void careerGoalsReportLiveProgressForAnAssignedPlayer() {
        WorldSession session = worldService.create(OWNER, 404L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Test", "Golfer", Nationality.USA, 20, Archetype.ALL_ROUNDER);
        worldService.setCareerGoals(OWNER, session.id(), List.of(CareerGoal.of(GoalType.WIN_A_MAJOR)));

        graphQlTester.document("query($id: ID!){ careerGoals(id: $id){ type target current achieved } }")
                .variable("id", session.id()).execute()
                .path("careerGoals").entityList(Object.class).hasSize(1)
                .path("careerGoals[0].type").entity(String.class).isEqualTo("WIN_A_MAJOR")
                .path("careerGoals[0].achieved").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void listSavesReturnsMetadata() {
        // advanceSeason autosaves; after it the reserved autosave should be listable.
        String id = worldService.create(OWNER, 505L, SMALL).id();
        worldService.advanceSeason(OWNER, id);

        graphQlTester.document("query { listSaves { saveId season week } }")
                .execute()
                .path("listSaves").entityList(Object.class).satisfies(saves ->
                        org.assertj.core.api.Assertions.assertThat(saves).isNotEmpty());
    }

    // --- Off-event safety ---

    @Test
    void playableEventReadsAreSafeOffEvent() {
        String id = worldService.create(OWNER, 606L, SMALL).id();
        graphQlTester.document("""
                        query($id: ID!){
                          currentSituation(id: $id){ holeNumber }
                          eventLeaderboard(id: $id){ position }
                          playerMadeCut(id: $id)
                        }
                        """)
                .variable("id", id).execute()
                .path("currentSituation").valueIsNull()
                .path("eventLeaderboard").entityList(Object.class).hasSize(0)
                .path("playerMadeCut").valueIsNull();
    }

    // --- Error classification ---

    @Test
    void unknownSessionYieldsNotFound() {
        graphQlTester.document("query { world(id: \"does-not-exist\"){ season } }")
                .execute()
                .errors()
                .expect(error -> error.getErrorType() == ErrorType.NOT_FOUND);
    }

    private static void assertPositive(Integer value) {
        org.assertj.core.api.Assertions.assertThat(value).isPositive();
    }

    private static void assertNonBlank(String value) {
        org.assertj.core.api.Assertions.assertThat(value).isNotBlank();
    }
}
