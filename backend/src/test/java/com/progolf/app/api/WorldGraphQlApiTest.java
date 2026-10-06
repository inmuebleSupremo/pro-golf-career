package com.progolf.app.api;

import com.progolf.app.world.WorldService;
import com.progolf.app.world.WorldSession;
import com.progolf.sim.control.CareerGoal;
import com.progolf.sim.control.GoalType;
import com.progolf.sim.core.Attribute;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.DevelopmentPoints;
import com.progolf.sim.world.WorldConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                          careerInbox(id: $id){ items { kind count } }
                          careerGoals(id: $id){ type }
                          pendingSponsorships(id: $id){ sponsor }
                          hallOfFame(id: $id){ golferId }
                        }
                        """)
                .variable("id", id).execute()
                .path("playerSchedule").entityList(Object.class).hasSize(0)
                .path("careerInbox.items").entityList(Object.class).hasSize(0)
                .path("careerGoals").entityList(Object.class).hasSize(0)
                .path("pendingSponsorships").entityList(Object.class).hasSize(0)
                .path("hallOfFame").entityList(Object.class).hasSize(0);
    }

    @Test
    void careerInboxIsAReadOnlyProjectionThatUpdatesAsCareerStateChanges() {
        WorldSession session = worldService.create(OWNER, 304L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Inbox", "Player", Nationality.USA, 20,
                Archetype.ALL_ROUNDER);

        List<CareerInboxRow> atSeasonStart = inboxRows(session.id());
        int scheduled = worldService.playerSchedule(OWNER, session.id()).size();
        org.assertj.core.api.Assertions.assertThat(scheduled).isPositive();
        org.assertj.core.api.Assertions.assertThat(atSeasonStart).contains(new CareerInboxRow("SCHEDULE", scheduled));
        org.assertj.core.api.Assertions.assertThat(worldService.status(OWNER, session.id()).week()).isEqualTo(1);

        // Reading the Inbox makes no change, while moving past the opening-week planning moment removes it.
        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id())).isEqualTo(atSeasonStart);
        worldService.playerSchedule(OWNER, session.id()).stream()
                .filter(entry -> entry.week() == 1)
                .forEach(entry -> worldService.skipEvent(OWNER, session.id(), entry.tournamentId()));
        worldService.advanceWeek(OWNER, session.id());
        org.assertj.core.api.Assertions.assertThat(worldService.status(OWNER, session.id()).week()).isGreaterThan(1);
        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id()))
                .noneMatch(item -> item.kind().equals("SCHEDULE"));
    }

    @Test
    void scheduleReviewAcknowledgementCompletesOnlyTheCurrentSeasonInboxItem() {
        WorldSession session = worldService.create(OWNER, 306L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Schedule", "Reviewer", Nationality.USA, 20,
                Archetype.ALL_ROUNDER);

        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id()))
                .extracting(CareerInboxRow::kind).contains("SCHEDULE");

        graphQlTester.document("mutation($id: ID!){ acknowledgeScheduleReview(id: $id) }")
                .variable("id", session.id()).execute()
                .path("acknowledgeScheduleReview").entity(Boolean.class).isEqualTo(true);

        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id()))
                .extracting(CareerInboxRow::kind).doesNotContain("SCHEDULE");

        worldService.advanceSeason(OWNER, session.id());
        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id()))
                .extracting(CareerInboxRow::kind).contains("SCHEDULE");
    }

    @Test
    void staffReviewAcknowledgementCompletesOnlyTheCurrentSeasonInboxItemWithoutChangingStaff() {
        WorldSession session = worldService.create(OWNER, 307L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Staff", "Reviewer", Nationality.USA, 20,
                Archetype.ALL_ROUNDER);
        worldService.advanceSeason(OWNER, session.id());

        var world = session.world();
        String playerId = world.playerGolferId().orElseThrow();
        var candidatesBefore = List.copyOf(world.pendingStaffOffers());
        double fundsBefore = world.financialAccountOf(playerId).availableFunds();
        int affordableCandidates = (int) candidatesBefore.stream()
                .filter(candidate -> candidate.hiringCost() <= fundsBefore)
                .count();
        org.assertj.core.api.Assertions.assertThat(affordableCandidates).isPositive();
        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id()))
                .contains(new CareerInboxRow("STAFF", affordableCandidates));

        graphQlTester.document("mutation($id: ID!){ acknowledgeStaffReview(id: $id) }")
                .variable("id", session.id()).execute()
                .path("acknowledgeStaffReview").entity(Boolean.class).isEqualTo(true);

        org.assertj.core.api.Assertions.assertThat(world.pendingStaffOffers()).isEqualTo(candidatesBefore);
        org.assertj.core.api.Assertions.assertThat(world.financialAccountOf(playerId).availableFunds()).isEqualTo(fundsBefore);
        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id()))
                .extracting(CareerInboxRow::kind).doesNotContain("STAFF");

        worldService.advanceSeason(OWNER, session.id());
        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id()))
                .extracting(CareerInboxRow::kind).contains("STAFF");
    }

    @Test
    void careerInboxAggregatesOnlyCurrentlyActionableCareerState() {
        WorldSession session = worldService.create(OWNER, 305L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Attention", "Player", Nationality.USA, 20,
                Archetype.ALL_ROUNDER);
        worldService.advanceSeason(OWNER, session.id());

        List<CareerInboxRow> items = inboxRows(session.id());
        org.assertj.core.api.Assertions.assertThat(items).extracting(CareerInboxRow::kind).doesNotHaveDuplicates();

        var world = session.world();
        String playerId = world.playerGolferId().orElseThrow();
        double funds = world.financialAccountOf(playerId).availableFunds();
        int schedule = world.currentWeek() == 1 ? world.playerSchedule().size() : 0;
        int development = allocatableDevelopmentRaises(session).isEmpty() ? 0 : world.playerDevelopmentPoints();
        int sponsorships = world.activeSponsorships().size() < world.maxConcurrentSponsorships()
                ? world.pendingSponsorships().size() : 0;
        int equipment = (int) world.pendingEquipmentOffers().stream().filter(item -> item.cost() <= funds).count()
                + world.pendingEquipmentDeals().size();
        int staff = (int) world.pendingStaffOffers().stream().filter(item -> item.hiringCost() <= funds).count();

        assertInboxCount(items, "SCHEDULE", schedule);
        assertInboxCount(items, "DEVELOPMENT", development);
        assertInboxCount(items, "SPONSORSHIP", sponsorships);
        assertInboxCount(items, "EQUIPMENT", equipment);
        assertInboxCount(items, "STAFF", staff);
    }

    private List<CareerInboxRow> inboxRows(String id) {
        return graphQlTester.document("query($id: ID!){ careerInbox(id: $id){ items { kind count } } }")
                .variable("id", id).execute()
                .path("careerInbox.items").entityList(CareerInboxRow.class).get();
    }

    /** A projection of the compact, current-state Inbox item returned by GraphQL. */
    record CareerInboxRow(String kind, int count) {
    }

    private static void assertInboxCount(List<CareerInboxRow> items, String kind, int expectedCount) {
        var matching = items.stream().filter(item -> item.kind().equals(kind)).toList();
        if (expectedCount == 0) {
            org.assertj.core.api.Assertions.assertThat(matching).isEmpty();
        } else {
            org.assertj.core.api.Assertions.assertThat(matching)
                    .containsExactly(new CareerInboxRow(kind, expectedCount));
        }
    }

    @Test
    void worldRankingsReturnOrderedNameEnrichedRowsAfterASeason() {
        String id = worldService.create(OWNER, 707L, SMALL).id();
        worldService.advanceSeason(OWNER, id); // resolve a season of events so ranking points accrue

        graphQlTester.document("""
                        query($id: ID!, $limit: Int){
                          worldRankings(id: $id, limit: $limit){ position golferId name rankingValue }
                        }
                        """)
                .variable("id", id).variable("limit", 5).execute()
                .path("worldRankings").entityList(Object.class).satisfies(rows ->
                        org.assertj.core.api.Assertions.assertThat(rows).isNotEmpty().hasSizeLessThanOrEqualTo(5))
                .path("worldRankings[0].position").entity(Integer.class).isEqualTo(1)
                .path("worldRankings[0].name").entity(String.class).satisfies(WorldGraphQlApiTest::assertNonBlank)
                .path("worldRankings[0].rankingValue").entity(Double.class).satisfies(value ->
                        org.assertj.core.api.Assertions.assertThat(value).isPositive());
    }

    @Test
    void recordsAreEmptyForAFreshWorldAndPopulateAfterASeason() {
        String id = worldService.create(OWNER, 808L, SMALL).id();
        graphQlTester.document("query($id: ID!){ records(id: $id){ type } }")
                .variable("id", id).execute()
                .path("records").entityList(Object.class).hasSize(0);

        worldService.advanceSeason(OWNER, id); // a season of events sets record holders

        graphQlTester.document("""
                        query($id: ID!){
                          records(id: $id){ type holderGolferId holderName value season }
                        }
                        """)
                .variable("id", id).execute()
                .path("records").entityList(Object.class).satisfies(rows ->
                        org.assertj.core.api.Assertions.assertThat(rows).isNotEmpty())
                .path("records[0].holderName").entity(String.class).satisfies(WorldGraphQlApiTest::assertNonBlank)
                .path("records[0].season").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive);
    }

    @Test
    void careerRecordsAreNullWithoutAPlayerAndPopulateAfterThePlayerCompetes() {
        String id = worldService.create(OWNER, 424L, SMALL).id();
        // No player assigned → the player-scoped records surface is null.
        graphQlTester.document("query($id: ID!){ careerRecords(id: $id){ summary { events } } }")
                .variable("id", id).execute()
                .path("careerRecords").valueIsNull();

        worldService.createPlayer(OWNER, id, "Rec", "Keeper", Nationality.USA, 20, Archetype.ALL_ROUNDER);
        worldService.advanceSeason(OWNER, id); // the player competes across a season, filling the ledger

        graphQlTester.document("""
                        query($id: ID!){
                          careerRecords(id: $id){
                            summary {
                              events wins majors runnerUps topTens cutsMade bestFinish careerEarnings
                              lowestRound { scoreToPar eventName location season date round }
                              lowestTournament { scoreToPar eventName location season date round }
                            }
                            events {
                              eventName location prestige tier tourTier appearances wins bestPosition
                              results { season date position scoreToPar won madeCut roundScores }
                            }
                          }
                        }
                        """)
                .variable("id", id).execute()
                .path("careerRecords.summary.events").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive)
                .path("careerRecords.summary.bestFinish").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive)
                // A single-round mark carries its round number; the event finished on a realistic golf date (Apr–Oct 2026+).
                .path("careerRecords.summary.lowestRound.round").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive)
                .path("careerRecords.summary.lowestRound.date").entity(String.class).satisfies(d ->
                        org.assertj.core.api.Assertions.assertThat(d).startsWith("20"))
                .path("careerRecords.events").entityList(Object.class).satisfies(rows ->
                        org.assertj.core.api.Assertions.assertThat(rows).isNotEmpty())
                .path("careerRecords.events[0].eventName").entity(String.class).satisfies(WorldGraphQlApiTest::assertNonBlank)
                .path("careerRecords.events[0].tourTier").entity(String.class).satisfies(WorldGraphQlApiTest::assertNonBlank)
                .path("careerRecords.events[0].appearances").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive)
                .path("careerRecords.events[0].results[0].date").entity(String.class).satisfies(WorldGraphQlApiTest::assertNonBlank)
                .path("careerRecords.events[0].results[0].season").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive);
    }

    @Test
    void staffOffersCarryProfileFieldsAndTheRosterIsQueryable() {
        WorldSession session = worldService.create(OWNER, 909L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Sam", "Rookie", Nationality.USA, 20, Archetype.ALL_ROUNDER);
        worldService.advanceSeason(OWNER, session.id()); // surfaces this season's staff offers

        graphQlTester.document("""
                        query($id: ID!){
                          playerStaff(id: $id){ role name }
                          pendingStaff(id: $id){ role name age nationality personality quality hiringCost seasonalSalary }
                        }
                        """)
                .variable("id", session.id()).execute()
                .path("playerStaff").entityList(Object.class).hasSize(0) // nothing hired yet
                .path("pendingStaff").entityList(Object.class).satisfies(rows ->
                        org.assertj.core.api.Assertions.assertThat(rows).isNotEmpty())
                .path("pendingStaff[0].name").entity(String.class).satisfies(n ->
                        org.assertj.core.api.Assertions.assertThat(n).contains(" ")) // a real name, not a serial
                .path("pendingStaff[0].age").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive)
                .path("pendingStaff[0].personality").entity(String.class).satisfies(WorldGraphQlApiTest::assertNonBlank);
    }

    @Test
    void developmentPointsAreEarnedSpendableAndReported() {
        WorldSession session = worldService.create(OWNER, 550L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Dev", "Prospect", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        worldService.advanceSeason(OWNER, session.id()); // earns Development Points from the season's play

        // Potential is exposed, and the season's play banked spendable Development Points (with cost curve).
        graphQlTester.document("""
                        query($id: ID!){
                          playerProfile(id: $id){ attributes { attribute value potential } }
                          playerDevelopment(id: $id){ points pointsPerRating costReference }
                        }
                        """)
                .variable("id", session.id()).execute()
                .path("playerProfile.attributes[0].potential").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive)
                .path("playerProfile.attributes").entityList(Object.class).satisfies(rows ->
                        org.assertj.core.api.Assertions.assertThat(rows).hasSize(Attribute.values().length))
                .path("playerDevelopment.points").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive)
                .path("playerDevelopment.costReference").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive);

        // Spend points to raise an attribute; the mutation returns the remaining balance.
        graphQlTester.document("""
                        mutation($id: ID!, $raises: [AttributeRaiseInput!]!){
                          spendDevelopmentPoints(id: $id, raises: $raises)
                        }
                        """)
                .variable("id", session.id())
                .variable("raises", List.of(Map.of("attribute", "PUTTING_ACCURACY", "points", 2)))
                .execute()
                .path("spendDevelopmentPoints").entity(Integer.class).satisfies(balance ->
                        org.assertj.core.api.Assertions.assertThat(balance).isGreaterThanOrEqualTo(0));

        // The spend records as development gains for the report.
        graphQlTester.document("query($id: ID!){ developmentReport(id: $id){ attribute delta } }")
                .variable("id", session.id()).execute()
                .path("developmentReport").entityList(Object.class).satisfies(rows ->
                        org.assertj.core.api.Assertions.assertThat(rows).isNotEmpty())
                .path("developmentReport[0].delta").entity(Integer.class).satisfies(WorldGraphQlApiTest::assertPositive);
    }

    @Test
    void appliedDevelopmentDisappearsFromInboxWhenNoFurtherRaiseIsAffordable() {
        WorldSession session = worldService.create(OWNER, 551L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Inbox", "Developer", Nationality.USA, 19,
                Archetype.ALL_ROUNDER);
        worldService.advanceSeason(OWNER, session.id());

        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id()))
                .extracting(CareerInboxRow::kind).contains("DEVELOPMENT");
        List<Map<String, Object>> raises = allocatableDevelopmentRaises(session);
        org.assertj.core.api.Assertions.assertThat(raises).isNotEmpty();

        graphQlTester.document("""
                        mutation($id: ID!, $raises: [AttributeRaiseInput!]!){
                          spendDevelopmentPoints(id: $id, raises: $raises)
                        }
                        """)
                .variable("id", session.id()).variable("raises", raises).execute()
                .path("spendDevelopmentPoints").entity(Integer.class).satisfies(balance ->
                        org.assertj.core.api.Assertions.assertThat(balance).isGreaterThanOrEqualTo(0));

        org.assertj.core.api.Assertions.assertThat(allocatableDevelopmentRaises(session)).isEmpty();
        org.assertj.core.api.Assertions.assertThat(inboxRows(session.id()))
                .extracting(CareerInboxRow::kind).doesNotContain("DEVELOPMENT");
    }

    /** Matches the Development screen's valid +1 choices until no banked point can fund another one. */
    private static List<Map<String, Object>> allocatableDevelopmentRaises(WorldSession session) {
        var world = session.world();
        var player = world.careerOf(world.playerGolferId().orElseThrow()).player();
        int remaining = world.playerDevelopmentPoints();
        Map<Attribute, Integer> raises = new java.util.EnumMap<>(Attribute.class);

        boolean added;
        do {
            added = false;
            for (Attribute attribute : Attribute.values()) {
                int rating = player.attributes().get(attribute) + raises.getOrDefault(attribute, 0);
                if (rating < player.potential().get(attribute)
                        && remaining >= DevelopmentPoints.costToRaise(rating)) {
                    raises.merge(attribute, 1, Integer::sum);
                    remaining -= DevelopmentPoints.costToRaise(rating);
                    added = true;
                    break;
                }
            }
        } while (added);

        List<Map<String, Object>> inputs = new ArrayList<>();
        for (Map.Entry<Attribute, Integer> raise : raises.entrySet()) {
            inputs.add(Map.of("attribute", raise.getKey().name(), "points", raise.getValue()));
        }
        return inputs;
    }

    @Test
    void seasonReviewSummarisesTheJustCompletedSeason() {
        WorldSession session = worldService.create(OWNER, 550L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Rev", "Player", Nationality.USA, 20, Archetype.ALL_ROUNDER);
        worldService.advanceSeason(OWNER, session.id()); // completes season 1; the world is now in season 2

        // Default (no season arg) reviews the most recently completed season (1). rankStart is null — there is
        // no season-0 snapshot to move from — and the season produced world news to headline.
        graphQlTester.document("""
                        query($id: ID!){
                          seasonReview(id: $id){
                            season rankStart rankEnd playerGolferId
                            stats { season events wins }
                            development { attribute delta season }
                            headlines { season type headline prominence subjectGolferId }
                          }
                        }
                        """)
                .variable("id", session.id()).execute()
                .path("seasonReview.season").entity(Integer.class).isEqualTo(1)
                .path("seasonReview.rankStart").valueIsNull()
                .path("seasonReview.stats.season").entity(Integer.class).isEqualTo(1)
                .path("seasonReview.playerGolferId").entity(String.class).satisfies(WorldGraphQlApiTest::assertNonBlank)
                .path("seasonReview.headlines").entityList(Object.class).satisfies(rows ->
                        org.assertj.core.api.Assertions.assertThat(rows).isNotEmpty())
                .path("seasonReview.headlines[0].season").entity(Integer.class).isEqualTo(1);
    }

    @Test
    void seasonReviewIsNullBeforeAnySeasonHasCompleted() {
        WorldSession session = worldService.create(OWNER, 551L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Fresh", "Start", Nationality.USA, 20, Archetype.ALL_ROUNDER);

        graphQlTester.document("query($id: ID!){ seasonReview(id: $id){ season } }")
                .variable("id", session.id()).execute()
                .path("seasonReview").valueIsNull();
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
    void achievementsReturnTheFullCatalogueLockedForANewPlayer() {
        WorldSession session = worldService.create(OWNER, 606L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Test", "Golfer", Nationality.USA, 20, Archetype.ALL_ROUNDER);

        graphQlTester.document("""
                        query($id: ID!){
                          achievements(id: $id){ id category categoryLabel title description secret unlocked seasonUnlocked }
                        }""")
                .variable("id", session.id()).execute()
                .path("achievements").entityList(Object.class).hasSize(18)
                .path("achievements[0].id").entity(String.class).isEqualTo("PRO_CARD")
                .path("achievements[0].category").entity(String.class).isEqualTo("MILESTONE")
                .path("achievements[0].unlocked").entity(Boolean.class).isEqualTo(false)
                // A secret achievement withholds its description while still locked.
                .path("achievements").entityList(AchievementRow.class).satisfies(rows -> {
                    AchievementRow snowman = rows.stream().filter(r -> r.id().equals("THE_SNOWMAN"))
                            .findFirst().orElseThrow();
                    org.assertj.core.api.Assertions.assertThat(snowman.secret()).isTrue();
                    org.assertj.core.api.Assertions.assertThat(snowman.description()).isNull();
                });
    }

    /** A projection of the Achievement GraphQL type for assertions. */
    record AchievementRow(String id, boolean secret, String description) {
    }

    @Test
    void achievementsAreEmptyForAWorldWithoutAPlayer() {
        String id = worldService.create(OWNER, 607L, SMALL).id();
        graphQlTester.document("query($id: ID!){ achievements(id: $id){ id } }")
                .variable("id", id).execute()
                .path("achievements").entityList(Object.class).hasSize(0);
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
                          playingHole(id: $id){ holeNumber }
                          eventLeaderboard(id: $id){ position }
                          playerMadeCut(id: $id)
                          playerPressure(id: $id)
                        }
                        """)
                .variable("id", id).execute()
                .path("currentSituation").valueIsNull()
                .path("playingHole").valueIsNull()
                .path("eventLeaderboard").entityList(Object.class).hasSize(0)
                .path("playerMadeCut").valueIsNull()
                .path("playerPressure").valueIsNull();
    }

    // --- Error classification ---

    @Test
    void unknownSessionYieldsNotFound() {
        graphQlTester.document("query { world(id: \"does-not-exist\"){ season } }")
                .execute()
                .errors()
                .expect(error -> error.getErrorType() == ErrorType.NOT_FOUND);
    }

    @Test
    void playerProfileReturnsTheGolfersIdentityAndBuild() {
        WorldSession session = worldService.create(OWNER, 5L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Ana", "Rivera", Nationality.ESP, 20,
                Archetype.SHORT_GAME_ARTIST);

        graphQlTester.document("""
                        query($id: ID!) {
                          playerProfile(id: $id) {
                            golferId firstName lastName nationality age archetype
                            careerEarnings availableFunds tour events wins topTens
                            attributes { attribute value }
                          }
                        }
                        """)
                .variable("id", session.id()).execute()
                .path("playerProfile.firstName").entity(String.class).isEqualTo("Ana")
                .path("playerProfile.lastName").entity(String.class).isEqualTo("Rivera")
                .path("playerProfile.nationality").entity(String.class).isEqualTo("ESP")
                .path("playerProfile.age").entity(Integer.class).isEqualTo(20)
                .path("playerProfile.archetype").entity(String.class).isEqualTo("SHORT_GAME_ARTIST")
                .path("playerProfile.tour").entity(String.class).isEqualTo("DEVELOPMENT")
                .path("playerProfile.events").entity(Integer.class).isEqualTo(0)
                .path("playerProfile.attributes").entityList(Object.class).satisfies(a -> assertPositive(a.size()));
    }

    @Test
    void playerFitnessReturnsHealthyStateForAFreshlyCreatedPlayer() {
        WorldSession session = worldService.create(OWNER, 8L, SMALL);
        worldService.createPlayer(OWNER, session.id(), "Ana", "Rivera", Nationality.ESP, 20,
                Archetype.ALL_ROUNDER);

        graphQlTester.document("""
                        query($id: ID!) {
                          playerFitness(id: $id) {
                            availability fitness fatigue canCompete canPlayThroughInjury
                            injury { type severity rehabWeeksRemaining }
                          }
                        }
                        """)
                .variable("id", session.id()).execute()
                .path("playerFitness.availability").entity(String.class).isEqualTo("AVAILABLE")
                .path("playerFitness.canCompete").entity(Boolean.class).isEqualTo(true)
                .path("playerFitness.fatigue").entity(Double.class).isEqualTo(0.0)
                .path("playerFitness.fitness").entity(Double.class).satisfies(value ->
                        org.assertj.core.api.Assertions.assertThat(value).isBetween(0.0, 1.0))
                .path("playerFitness.injury").valueIsNull();
    }

    @Test
    void playerFitnessIsNullWithoutAPlayer() {
        WorldSession session = worldService.create(OWNER, 9L, SMALL);
        graphQlTester.document("query($id: ID!){ playerFitness(id: $id){ availability } }")
                .variable("id", session.id()).execute()
                .path("playerFitness").valueIsNull();
    }

    @Test
    void playerProfileIsNullWithoutAPlayer() {
        WorldSession session = worldService.create(OWNER, 6L, SMALL);
        graphQlTester.document("query($id: ID!){ playerProfile(id: $id){ golferId } }")
                .variable("id", session.id()).execute()
                .path("playerProfile").valueIsNull();
    }

    private static void assertPositive(Integer value) {
        org.assertj.core.api.Assertions.assertThat(value).isPositive();
    }

    private static void assertNonBlank(String value) {
        org.assertj.core.api.Assertions.assertThat(value).isNotBlank();
    }
}
