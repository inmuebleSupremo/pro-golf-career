package com.progolf.app.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.world.WorldService;
import com.progolf.app.world.WorldSession;
import com.progolf.sim.world.WorldConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * graphql-api (add-graphql-mutations): the write side — player-control decisions, playing/simming a player
 * event, and persistence writes — over GraphQL, plus BAD_REQUEST/NOT_FOUND error classification. Preconditions
 * (an assigned player, a pending event, pending offers) are arranged through {@link WorldService} directly,
 * under the same authenticated owner ({@link #OWNER}) the resolvers scope to (spec: resource-ownership).
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@WithMockUser(username = WorldGraphQlMutationsTest.OWNER)
class WorldGraphQlMutationsTest {

    static final String OWNER = "owner-mutations-test";

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private WorldService worldService;

    // --- Player control ---

    @Test
    void createPlayerThenSetAndReadGoals() {
        String id = worldService.create(OWNER, 1001L, SMALL).id();

        String golferId = graphQlTester.document("""
                        mutation($id: ID!){
                          createPlayer(id: $id, firstName: "Test", lastName: "Golfer",
                                       nationality: "USA", startAge: 20, archetype: "ALL_ROUNDER")
                        }
                        """)
                .variable("id", id).execute()
                .path("createPlayer").entity(String.class).get();
        assertThat(golferId).isNotBlank();

        graphQlTester.document("""
                        mutation($id: ID!){ setCareerGoals(id: $id, goals: [{type: "WIN_A_MAJOR"}]) }
                        """)
                .variable("id", id).execute()
                .path("setCareerGoals").entity(Boolean.class).isEqualTo(true);

        graphQlTester.document("query($id: ID!){ careerGoals(id: $id){ type achieved } }")
                .variable("id", id).execute()
                .path("careerGoals[0].type").entity(String.class).isEqualTo("WIN_A_MAJOR")
                .path("careerGoals[0].achieved").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void standingDecisionsAreApplied() {
        WorldSession session = worldService.create(OWNER, 1002L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Dev", "Focus", com.progolf.sim.player.Nationality.USA, 20,
                com.progolf.sim.player.Archetype.ALL_ROUNDER);

        graphQlTester.document("""
                        mutation($id: ID!){
                          resting: setResting(id: $id, resting: true)
                          focus: setDevelopmentFocus(id: $id, focus: ["PUTTING_ACCURACY", "DRIVING_DISTANCE"])
                        }
                        """)
                .variable("id", session.id()).execute()
                .path("resting").entity(Boolean.class).isEqualTo(true)
                .path("focus").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void aPendingStaffCandidateCanBeHired() {
        WorldSession session = worldService.create(OWNER, 1003L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Boss", "Player", com.progolf.sim.player.Nationality.USA, 20,
                com.progolf.sim.player.Archetype.ALL_ROUNDER);
        worldService.advanceSeason(OWNER, session.id()); // generates a pending candidate for every open role

        graphQlTester.document("query($id: ID!){ pendingStaff(id: $id){ role } }")
                .variable("id", session.id()).execute()
                .path("pendingStaff").entityList(Object.class).satisfies(l -> assertThat(l).isNotEmpty());

        // hireStaff delegates and confirms; whether the hire lands depends on affordability, but the call succeeds.
        graphQlTester.document("mutation($id: ID!){ hireStaff(id: $id, index: 0) }")
                .variable("id", session.id()).execute()
                .path("hireStaff").entity(Boolean.class).isEqualTo(true);
    }

    // --- Playable event ---

    @Test
    void aPlayerEventIsPlayedAndCompleted() {
        WorldSession session = worldService.create(OWNER, 20L, SMALL);
        String golferId = session.world().activeGolferIds().get(0); // strongest golfer — a regular in fields
        worldService.assignPlayer(OWNER, session.id(), golferId);

        int guard = 0;
        while (!worldService.hasPendingEvent(OWNER, session.id()) && guard++ < 60) {
            worldService.advanceWeek(OWNER, session.id());
        }
        assertThat(worldService.hasPendingEvent(OWNER, session.id())).as("player event should come up").isTrue();

        double distance = worldService.currentSituation(OWNER, session.id()).distanceToPin();
        graphQlTester.document("""
                        mutation($id: ID!, $d: Float!){
                          playShot(id: $id, decision: {club: "DRIVER", targetDistance: $d, strategy: "BALANCED"}){
                            finalSurface strokes
                          }
                        }
                        """)
                .variable("id", session.id()).variable("d", distance).execute()
                .path("playShot.strokes").entity(Integer.class).satisfies(s -> assertThat(s).isGreaterThanOrEqualTo(1))
                .path("playShot.finalSurface").entity(String.class).satisfies(s -> assertThat(s).isNotBlank());

        graphQlTester.document("mutation($id: ID!){ simEvent(id: $id) }")
                .variable("id", session.id()).execute()
                .path("simEvent").entity(Boolean.class).isEqualTo(true);

        graphQlTester.document("mutation($id: ID!){ completeEvent(id: $id){ hasPendingEvent } }")
                .variable("id", session.id()).execute()
                .path("completeEvent.hasPendingEvent").entity(Boolean.class).isEqualTo(false);
    }

    // --- Persistence writes ---

    @Test
    void saveThenLoadThenDelete() {
        WorldSession session = worldService.create(OWNER, 30L, SMALL);
        worldService.advanceWeek(OWNER, session.id());
        int week = worldService.status(OWNER, session.id()).week();

        graphQlTester.document("mutation($id: ID!){ save(id: $id, saveId: \"gql-save\") }")
                .variable("id", session.id()).execute()
                .path("save").entity(Boolean.class).isEqualTo(true);

        graphQlTester.document("mutation { load(saveId: \"gql-save\"){ id week season } }")
                .execute()
                .path("load.week").entity(Integer.class).isEqualTo(week)
                .path("load.id").entity(String.class).satisfies(id -> assertThat(id).isNotBlank());

        graphQlTester.document("mutation { deleteSave(saveId: \"gql-save\") }")
                .execute()
                .path("deleteSave").entity(Boolean.class).isEqualTo(true);

        graphQlTester.document("mutation { load(saveId: \"gql-save\"){ id } }")
                .execute().errors()
                .expect(error -> error.getErrorType() == ErrorType.NOT_FOUND);
    }

    // --- Error classification ---

    @Test
    void offEventPlayShotIsBadRequest() {
        String id = worldService.create(OWNER, 40L, SMALL).id();
        graphQlTester.document("""
                        mutation($id: ID!){
                          playShot(id: $id, decision: {club: "DRIVER", targetDistance: 250, strategy: "BALANCED"}){ strokes }
                        }
                        """)
                .variable("id", id).execute().errors()
                .expect(error -> error.getErrorType() == ErrorType.BAD_REQUEST);
    }

    @Test
    void badEnumNameIsBadRequest() {
        String id = worldService.create(OWNER, 41L, SMALL).id();
        graphQlTester.document("""
                        mutation($id: ID!){
                          createPlayer(id: $id, firstName: "Bad", lastName: "Archetype",
                                       nationality: "USA", startAge: 20, archetype: "NOT_A_REAL_ARCHETYPE")
                        }
                        """)
                .variable("id", id).execute().errors()
                .expect(error -> error.getErrorType() == ErrorType.BAD_REQUEST);
    }

    @Test
    void outOfRangeOfferIndexIsBadRequest() {
        WorldSession session = worldService.create(OWNER, 42L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Idx", "Range", com.progolf.sim.player.Nationality.USA, 20,
                com.progolf.sim.player.Archetype.ALL_ROUNDER);

        graphQlTester.document("mutation($id: ID!){ acceptSponsorship(id: $id, index: 99) }")
                .variable("id", session.id()).execute().errors()
                .expect(error -> error.getErrorType() == ErrorType.BAD_REQUEST);
    }

    @Test
    void unknownSaveLoadIsNotFound() {
        graphQlTester.document("mutation { load(saveId: \"does-not-exist\"){ id } }")
                .execute().errors()
                .expect(error -> error.getErrorType() == ErrorType.NOT_FOUND);
    }
}
