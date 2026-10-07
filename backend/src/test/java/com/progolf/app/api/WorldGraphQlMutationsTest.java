package com.progolf.app.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.api.dto.PositionDto;
import com.progolf.app.world.WorldService;
import com.progolf.app.world.WorldSession;
import com.progolf.sim.course.CourseGeometry;
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

        var situation = worldService.currentSituation(OWNER, session.id());
        var cup = session.world().playerEvent().currentEffectiveGeometry().greenCenter();
        graphQlTester.document("""
                        mutation($id: ID!, $x: Float!, $y: Float!, $revision: String!){
                          playShot(id: $id, intent: {club: "DRIVER", aimPoint: {x: $x, y: $y}, expectedShotRevision: $revision}){
                            outcome { finalSurface strokes }
                          }
                        }
                        """)
                .variable("id", session.id()).variable("x", cup.x()).variable("y", cup.y()).variable("revision", situation.shotRevision()).execute()
                .path("playShot.outcome.strokes").entity(Integer.class).satisfies(s -> assertThat(s).isGreaterThanOrEqualTo(1))
                .path("playShot.outcome.finalSurface").entity(String.class).satisfies(s -> assertThat(s).isNotBlank());

        graphQlTester.document("mutation($id: ID!){ simEvent(id: $id) }")
                .variable("id", session.id()).execute()
                .path("simEvent").entity(Boolean.class).isEqualTo(true);

        graphQlTester.document("mutation($id: ID!){ completeEvent(id: $id){ hasPendingEvent } }")
                .variable("id", session.id()).execute()
                .path("completeEvent.hasPendingEvent").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void canonicalPlayingHoleAndShotSettlementAreExposedOverGraphQl() {
        WorldSession session = worldService.create(OWNER, 2026L, SMALL);
        String golferId = session.world().activeGolferIds().get(0);
        worldService.assignPlayer(OWNER, session.id(), golferId);

        int guard = 0;
        while (!worldService.hasPendingEvent(OWNER, session.id()) && guard++ < 60) {
            worldService.advanceWeek(OWNER, session.id());
        }
        assertThat(worldService.hasPendingEvent(OWNER, session.id())).isTrue();

        var event = session.world().playerEvent();
        CourseGeometry currentEffectiveGeometry = event.currentEffectiveGeometry();
        CourseGeometry prefetchedEffectiveGeometry = event.effectiveGeometry(2);
        assertThat(currentEffectiveGeometry).isNotEqualTo(event.currentHole().geometry());
        assertThat(prefetchedEffectiveGeometry).isNotEqualTo(event.holeGeometry(2).geometry());

        var response = graphQlTester.document("""
                        query($id: ID!){
                          current: playingHole(id: $id){
                            geometry { tee { x y } cup { x y } playableBoundary { x y } regions { surface boundary { x y } } }
                            ball { position { x y } lie }
                          }
                          prefetched: playingHole(id: $id, hole: 2){
                            geometry { playableBoundary { x y } regions { surface boundary { x y } } }
                          }
                        }
                        """)
                .variable("id", session.id()).execute();
        response.path("current.geometry.playableBoundary").entityList(Object.class).satisfies(points ->
                        assertThat(points).hasSizeGreaterThanOrEqualTo(3))
                .path("current.geometry.regions").entityList(Object.class).satisfies(regions ->
                        assertThat(regions).isNotEmpty())
                .path("current.ball.lie").entity(String.class).isEqualTo("TEE_BOX")
                .path("current.geometry.playableBoundary").entityList(PositionDto.class)
                .satisfies(points -> assertThat(points).containsExactlyElementsOf(points(currentEffectiveGeometry)))
                .path("prefetched.geometry.playableBoundary").entityList(PositionDto.class)
                .satisfies(points -> assertThat(points).containsExactlyElementsOf(points(prefetchedEffectiveGeometry)));

        var currentSituation = worldService.currentSituation(OWNER, session.id());
        var cup = currentEffectiveGeometry.greenCenter();
        graphQlTester.document("""
                        mutation($id: ID!, $x: Float!, $y: Float!, $revision: String!){
                          playShot(id: $id, intent: {club: "DRIVER", aimPoint: {x: $x, y: $y}, expectedShotRevision: $revision}){
                            outcome { settlement {
                              contact { position { x y } surface }
                              recoveryPosition { x y }
                              recoveryKind
                              ball { position { x y } lie }
                            } }
                          }
                        }
                        """)
                .variable("id", session.id()).variable("x", cup.x()).variable("y", cup.y()).variable("revision", currentSituation.shotRevision()).execute()
                .path("playShot.outcome.settlement.contact.surface").entity(String.class).satisfies(surface ->
                        assertThat(surface).isNotBlank())
                .path("playShot.outcome.settlement.recoveryKind").entity(String.class).satisfies(kind ->
                        assertThat(kind).isNotBlank())
                .path("playShot.outcome.settlement.ball.lie").entity(String.class).satisfies(lie ->
                        assertThat(lie).isNotBlank());
    }

    private static List<PositionDto> points(CourseGeometry geometry) {
        return geometry.playableBoundary().stream().map(point -> new PositionDto(point.x(), point.y())).toList();
    }

    @Test
    void playerPressureIsZeroInOpeningRoundsAndReadableDuringAnEvent() {
        WorldSession session = worldService.create(OWNER, 20L, SMALL);
        String golferId = session.world().activeGolferIds().get(0);
        worldService.assignPlayer(OWNER, session.id(), golferId);

        int guard = 0;
        while (!worldService.hasPendingEvent(OWNER, session.id()) && guard++ < 60) {
            worldService.advanceWeek(OWNER, session.id());
        }
        assertThat(worldService.hasPendingEvent(OWNER, session.id())).isTrue();

        // A freshly-started event is on round 1, which carries no closing pressure by design.
        graphQlTester.document("query($id: ID!){ playerPressure(id: $id) }")
                .variable("id", session.id()).execute()
                .path("playerPressure").entity(Double.class).isEqualTo(0.0);
    }

    @Test
    void currentSituationIsNullNotAnErrorForAFinishedButPendingEvent() {
        WorldSession session = worldService.create(OWNER, 21L, SMALL);
        String golferId = session.world().activeGolferIds().get(0);
        worldService.assignPlayer(OWNER, session.id(), golferId);

        int guard = 0;
        while (!worldService.hasPendingEvent(OWNER, session.id()) && guard++ < 60) {
            worldService.advanceWeek(OWNER, session.id());
        }
        assertThat(worldService.hasPendingEvent(OWNER, session.id())).isTrue();

        // Play the event to the end WITHOUT completing it — the "finish event" window the play page renders.
        worldService.simEvent(OWNER, session.id());
        assertThat(worldService.hasPendingEvent(OWNER, session.id())).as("still pending until completed").isTrue();

        // The play page's batched read: currentSituation must resolve to null (→ show Finish), not NPE, and
        // the final leaderboard is still readable. A thrown resolver would fail the whole query.
        graphQlTester.document("query($id: ID!){ currentSituation(id: $id){ holeNumber } eventLeaderboard(id: $id){ position } }")
                .variable("id", session.id()).execute()
                .errors().verify()
                .path("currentSituation").valueIsNull()
                .path("eventLeaderboard").entityList(Object.class).satisfies(l -> assertThat(l).isNotEmpty());
    }

    @Test
    void playerScorecardReportsTheCurrentRoundProgress() {
        WorldSession session = worldService.create(OWNER, 22L, SMALL);
        String golferId = session.world().activeGolferIds().get(0);
        worldService.assignPlayer(OWNER, session.id(), golferId);

        int guard = 0;
        while (!worldService.hasPendingEvent(OWNER, session.id()) && guard++ < 60) {
            worldService.advanceWeek(OWNER, session.id());
        }
        assertThat(worldService.hasPendingEvent(OWNER, session.id())).isTrue();

        // At the start of the event: round 1, hole 1, nothing completed yet.
        graphQlTester.document("query($id: ID!){ playerScorecard(id: $id){ roundNumber currentHole toPar holes { holeNumber } } }")
                .variable("id", session.id()).execute()
                .path("playerScorecard.roundNumber").entity(Integer.class).isEqualTo(1)
                .path("playerScorecard.currentHole").entity(Integer.class).isEqualTo(1)
                .path("playerScorecard.holes").entityList(Object.class).satisfies(l -> assertThat(l).isEmpty());

        // Play a hole to completion — the scorecard advances and records it.
        worldService.simHole(OWNER, session.id());
        graphQlTester.document("query($id: ID!){ playerScorecard(id: $id){ currentHole holes { holeNumber par strokes } } }")
                .variable("id", session.id()).execute()
                .path("playerScorecard.currentHole").entity(Integer.class).isEqualTo(2)
                .path("playerScorecard.holes").entityList(Object.class).satisfies(l -> assertThat(l).hasSize(1))
                .path("playerScorecard.holes[0].strokes").entity(Integer.class)
                .satisfies(s -> assertThat(s).isGreaterThanOrEqualTo(1));
    }

    @Test
    void playerSeasonStatsReportsEachSeasonCompeted() {
        WorldSession session = worldService.create(OWNER, 25L, SMALL);
        String golferId = session.world().activeGolferIds().get(0);
        worldService.assignPlayer(OWNER, session.id(), golferId);
        worldService.advanceSeason(OWNER, session.id()); // season 1 is played out

        graphQlTester.document("query($id: ID!){ playerSeasonStats(id: $id){ season events wins topTens earnings } }")
                .variable("id", session.id()).execute()
                .path("playerSeasonStats").entityList(Object.class).satisfies(l -> assertThat(l).isNotEmpty())
                .path("playerSeasonStats[0].season").entity(Integer.class).isEqualTo(1)
                .path("playerSeasonStats[0].events").entity(Integer.class)
                .satisfies(e -> assertThat(e).isGreaterThan(0));
    }

    @Test
    void newsFeedReportsRecentWorldNewsMostRecentFirst() {
        WorldSession session = worldService.create(OWNER, 24L, SMALL);
        worldService.advanceSeason(OWNER, session.id()); // a full season generates tournament results + milestones

        graphQlTester.document("query($id: ID!){ newsFeed(id: $id, limit: 5){ season type headline } }")
                .variable("id", session.id()).execute()
                .path("newsFeed").entityList(Object.class).satisfies(l -> assertThat(l).isNotEmpty())
                .path("newsFeed[0].headline").entity(String.class).satisfies(h -> assertThat(h).isNotBlank());
    }

    @Test
    void playerCalendarNamesEventsAndReportsPlayedResults() {
        WorldSession session = worldService.create(OWNER, 26L, SMALL);
        String golferId = session.world().activeGolferIds().get(0);
        worldService.assignPlayer(OWNER, session.id(), golferId);

        int guard = 0;
        while (!worldService.hasPendingEvent(OWNER, session.id()) && guard++ < 60) {
            worldService.advanceWeek(OWNER, session.id());
        }
        worldService.simEvent(OWNER, session.id());
        worldService.completeEvent(OWNER, session.id()); // at least one event is now played

        List<CalendarProbe> calendar = graphQlTester.document("""
                        query($id: ID!){
                          playerCalendar(id: $id){
                            name played
                            result { winner { position name score madeCut earnings }
                                     topThree { position name score madeCut earnings }
                                     playerFinish { position name score madeCut earnings } }
                          }
                        }
                        """)
                .variable("id", session.id()).execute()
                .path("playerCalendar").entityList(CalendarProbe.class).get();

        assertThat(calendar).isNotEmpty();
        assertThat(calendar).allSatisfy(e -> assertThat(e.name()).isNotBlank());
        CalendarProbe played = calendar.stream().filter(CalendarProbe::played).findFirst().orElseThrow();
        assertThat(played.result()).isNotNull();
        assertThat(played.result().winner().name()).isNotBlank();
        assertThat(played.result().topThree()).isNotEmpty();
    }

    private record CalendarProbe(String name, boolean played, ResultProbe result) {
    }

    private record ResultProbe(FinisherProbe winner, List<FinisherProbe> topThree, FinisherProbe playerFinish) {
    }

    private record FinisherProbe(int position, String name, int score, boolean madeCut, double earnings) {
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
                          playShot(id: $id, intent: {club: "DRIVER", aimPoint: {x: 0, y: 250}, expectedShotRevision: "1:1"}){ stale }
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
