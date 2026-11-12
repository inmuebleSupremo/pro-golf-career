package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;
import java.util.List;
import org.junit.jupiter.api.Test;

class DevelopmentTourCatalogueTest {
    private static final WorldConfig SMALL_STANDARD = new WorldConfig(8, 30, 1, 4, 12, 4, 1);

    @Test
    void catalogueMatchesApprovedCalendarExactly() {
        assertThat(DevelopmentTourCatalogue.events()).extracting(e ->
                e.week() + "|" + e.name() + "|" + e.location() + "|" + e.classification()
                        + "|" + e.prestige() + "|" + e.hostPolicy()).containsExactly(
                "1|Carolina Gateway Open|North Carolina, USA|PARKLAND|REGULAR|ROTATING",
                "2|Sandhills Classic|Pinehurst region, North Carolina, USA|HEATHLAND|REGULAR|FIXED",
                "3|Lowcountry Coastal Open|Kiawah Island, South Carolina, USA|LINKS|REGULAR|ROTATING",
                "4|Piedmont Challenge|North Carolina / Georgia, USA|WOODLAND|REGULAR|ROTATING",
                "5|Southeastern Invitational|Georgia, USA|PARKLAND|SIGNATURE|FIXED",
                "8|Ohio Valley Open|Ohio, USA|PARKLAND|REGULAR|ROTATING",
                "9|Lake Michigan Dunes Classic|Michigan, USA|LINKS|REGULAR|ROTATING",
                "10|Pine Barrens Open|New Jersey, USA|HEATHLAND|REGULAR|ROTATING",
                "12|Hudson Valley Open|New York, USA|WOODLAND|REGULAR|ROTATING",
                "13|Westchester Championship|New York, USA|PARKLAND|REGULAR|ROTATING",
                "15|Ardennes Woodland Open|Belgium|WOODLAND|REGULAR|ROTATING",
                "17|Surrey Heath Invitational|Surrey, England|HEATHLAND|SIGNATURE|ROTATING",
                "18|Northumbria Links Classic|Northumberland, England|LINKS|REGULAR|ROTATING",
                "19|Irish Midlands Open|County Kildare, Ireland|HEATHLAND|REGULAR|ROTATING",
                "20|Fife Coast Championship|Fife, Scotland|LINKS|REGULAR|ROTATING",
                "23|Pacific Coast Classic|Monterey, California, USA|LINKS|REGULAR|ROTATING",
                "24|Coachella Valley Classic|California, USA|DESERT|REGULAR|ROTATING",
                "25|Sonoran Invitational|Arizona, USA|DESERT|SIGNATURE|ROTATING",
                "26|Rio Grande Valley Open|Albuquerque, New Mexico, USA|PARKLAND|REGULAR|ROTATING",
                "29|Development Tour Championship|Carolinas, USA|PARKLAND|TOUR_CHAMPIONSHIP|FIXED");
        assertThat(DevelopmentTourCatalogue.events()).extracting(DevelopmentTourCatalogue.Event::id)
                .doesNotHaveDuplicates();
        assertThat(DevelopmentTourCatalogue.events()).filteredOn(e -> e.prestige() == EventPrestige.REGULAR).hasSize(16);
        assertThat(DevelopmentTourCatalogue.events()).filteredOn(e -> e.prestige() == EventPrestige.SIGNATURE).hasSize(3);
    }

    @Test
    void venueRecipesHaveApprovedCapacityAndGeography() {
        assertThat(DevelopmentTourCatalogue.venues()).hasSize(54)
                .extracting(DevelopmentTourCatalogue.Venue::id).doesNotHaveDuplicates();
        for (var event : DevelopmentTourCatalogue.events()) {
            List<Integer> indices = DevelopmentTourCatalogue.eligibleIndices(event.id());
            assertThat(indices).hasSize(event.hostPolicy() == DevelopmentTourCatalogue.HostPolicy.FIXED ? 1 : 3);
            for (int index : indices) {
                var venue = DevelopmentTourCatalogue.venue(index);
                assertThat(venue.eventId()).isEqualTo(event.id());
                assertThat(venue.classification()).isEqualTo(event.classification());
                assertThat(venue.country()).isEqualTo(event.country());
                assertThat(event.eligibleRegions()).contains(venue.region());
            }
            assertThat(DevelopmentTourCatalogue.selectedVenueIndex(event, 47L, 1))
                    .isEqualTo(DevelopmentTourCatalogue.selectedVenueIndex(event, 47L, 1));
            if (indices.size() == 3) {
                assertThat(List.of(DevelopmentTourCatalogue.selectedVenueIndex(event, 47L, 1),
                        DevelopmentTourCatalogue.selectedVenueIndex(event, 47L, 2),
                        DevelopmentTourCatalogue.selectedVenueIndex(event, 47L, 3))).containsExactlyInAnyOrderElementsOf(indices);
                assertThat(DevelopmentTourCatalogue.selectedVenueIndex(event, 47L, 4))
                        .isEqualTo(DevelopmentTourCatalogue.selectedVenueIndex(event, 47L, 1));
            } else {
                assertThat(DevelopmentTourCatalogue.selectedVenueIndex(event, 47L, 10)).isEqualTo(indices.getFirst());
            }
        }
    }

    @Test
    void scheduledHostsAreUniqueLazyAndStableAfterRestore() {
        World world = World.create(47L, SMALL_STANDARD);
        assertThat(world.developmentCatalogueVersion()).isEqualTo(1);
        List<ScheduledTournament> development = world.currentSchedule().stream()
                .filter(e -> e.tier() == TourTier.DEVELOPMENT).toList();
        assertThat(development).hasSize(20);
        assertThat(development.stream().map(ScheduledTournament::courseIndex).toList()).doesNotHaveDuplicates();
        assertThat(development.stream().map(ScheduledTournament::catalogueId).toList()).doesNotHaveDuplicates();
        assertThat(development.stream().map(e -> DevelopmentTourCatalogue.venue(e.courseIndex() - 12).id()).toList())
                .doesNotHaveDuplicates();
        assertThat(world.generatedDevelopmentCourseCount()).isZero();
        for (ScheduledTournament event : development) {
            var identity = DevelopmentTourCatalogue.forId(event.catalogueId());
            assertThat(world.nameFor(event)).isEqualTo(identity.name());
            assertThat(world.locationFor(event)).isEqualTo(identity.location());
            assertThat(world.courseTypeFor(event)).isEqualTo(identity.classification().name());
            assertThat(world.hostIdentityFor(event).region()).isIn(identity.eligibleRegions());
            assertThat(world.hostIdentityFor(event).name()).isNotEqualTo(identity.name());
        }
        assertThat(world.generatedDevelopmentCourseCount()).isZero();
        ScheduledTournament hostEvent = development.get(3);
        Course host = world.courseFor(hostEvent.courseIndex());
        assertThat(host.identity()).isEqualTo(world.hostIdentityFor(hostEvent));
        assertThat(world.courseFor(hostEvent.courseIndex())).isSameAs(host);
        assertThat(world.generatedDevelopmentCourseCount()).isEqualTo(1);
        assertThat(host.identity().classification()).isEqualTo(EnvironmentClassification.WOODLAND);
        assertThat(world.courseFamilyFor(host.identity().classification())).isEqualTo("PARKLAND");
        assertThat(host.identity().region()).isIn("North Carolina", "Georgia");
        World restored = World.restore(47L, SMALL_STANDARD, world.snapshot());
        assertThat(restored.currentSchedule()).isEqualTo(world.currentSchedule());
        assertThat(restored.generatedDevelopmentCourseCount()).isZero();
        assertThat(restored.courseFor(hostEvent.courseIndex())).isEqualTo(host);
        assertThat(World.create(47L, SMALL_STANDARD).currentSchedule()).isEqualTo(world.currentSchedule());
        assertThat(World.create(48L, SMALL_STANDARD).currentSchedule().stream()
                .filter(e -> e.tier() == TourTier.DEVELOPMENT).map(e -> e.courseIndex()).toList())
                .doesNotHaveDuplicates();
    }

    @Test
    void generatorSevenEightAndNineRetainCatalogueClassification() {
        for (int version : new int[] {CourseGenConstants.V5_REFINED_GENERATOR_VERSION,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION,
                CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION}) {
            World world = World.create(91L, SMALL_STANDARD, version);
            assertThat(world.developmentCatalogueVersion()).isEqualTo(1);
            assertThat(world.currentSchedule().stream().filter(e -> e.tier() == TourTier.DEVELOPMENT)
                    .map(ScheduledTournament::catalogueId).toList()).hasSize(20).doesNotContainNull();
            var sandhills = world.currentSchedule().stream()
                    .filter(e -> "sandhills-classic".equals(e.catalogueId())).findFirst().orElseThrow();
            assertThat(world.courseFor(sandhills.courseIndex()).identity().classification())
                    .isEqualTo(EnvironmentClassification.HEATHLAND);
            assertThat(world.courseFor(sandhills.courseIndex()).generatorVersion()).isEqualTo(version);
        }
    }

    @Test
    void playerEventKeepsCatalogueAndHostIdentityThroughResultAndRecord() {
        World world = World.create(201L, SMALL_STANDARD);
        String playerId = world.createPlayer("Tour", "Golfer", Nationality.USA, 22, Archetype.SHORT_GAME_ARTIST);
        ScheduledTournament opening = world.currentSchedule().stream()
                .filter(e -> "carolina-gateway".equals(e.catalogueId())).findFirst().orElseThrow();
        var host = world.hostIdentityFor(opening);
        assertThat(world.playerSchedule()).filteredOn(entry -> entry.tournamentId() == opening.tournamentId())
                .singleElement().satisfies(entry -> {
            assertThat(entry.name()).isEqualTo("Carolina Gateway Open");
            assertThat(entry.location()).isEqualTo("North Carolina, USA");
            assertThat(entry.hostName()).isEqualTo(host.name());
            assertThat(entry.hostRegion()).isEqualTo(host.region());
        });
        world.advanceWeek();
        assertThat(world.hasPendingPlayerEvent()).isTrue();
        assertThat(world.currentEventScene()).hasValueSatisfying(scene -> {
            assertThat(scene.name()).isEqualTo("Carolina Gateway Open");
            assertThat(scene.hostName()).isEqualTo(host.name());
        });
        world.playerEvent().simEvent();
        world.completePlayerEvent();
        assertThat(world.resultOf(opening.tournamentId()).orElseThrow().tournamentName())
                .isEqualTo("Carolina Gateway Open");
        assertThat(world.careerStatisticsOf(playerId).events()).isEqualTo(1);
        double awardedPrize = world.resultOf(opening.tournamentId()).orElseThrow().finishingOrder().stream()
                .filter(f -> f.golfer().player().id().equals(playerId)).findFirst().orElseThrow().prize();
        assertThat(world.financialAccountOf(playerId).tournamentEarnings()).isEqualTo(awardedPrize);
        assertThat(world.snapshot().playerCareerRecords().records()).anySatisfy(record -> {
            assertThat(record.catalogueId()).isEqualTo(opening.catalogueId());
            assertThat(record.hostName()).isEqualTo(host.name());
            assertThat(record.hostRegion()).isEqualTo(host.region());
        });
    }

    @Test
    void seasonTransitionKeepsTournamentIdentityAndFixedHostsWhileRotatingOthers() {
        World world = World.create(53L, SMALL_STANDARD);
        List<ScheduledTournament> first = world.currentSchedule();
        world.advanceSeason();
        assertThat(world.developmentCatalogueVersion()).isEqualTo(1);
        assertThat(world.archives()).hasSize(1);
        assertThat(world.archives().getFirst().schedule()).isEqualTo(first);
        for (ScheduledTournament original : first) {
            if (original.tier() != TourTier.DEVELOPMENT) continue;
            ScheduledTournament next = world.currentSchedule().stream()
                    .filter(e -> original.catalogueId().equals(e.catalogueId())).findFirst().orElseThrow();
            assertThat(next.tournamentId()).isNotEqualTo(original.tournamentId());
            assertThat(world.nameFor(next)).isEqualTo(world.nameFor(original));
            assertThat(world.locationFor(next)).isEqualTo(world.locationFor(original));
            var policy = DevelopmentTourCatalogue.forId(original.catalogueId()).hostPolicy();
            if (policy == DevelopmentTourCatalogue.HostPolicy.FIXED) {
                assertThat(next.courseIndex()).isEqualTo(original.courseIndex());
            } else {
                assertThat(next.courseIndex()).isNotEqualTo(original.courseIndex());
            }
        }
        assertThat(world.currentSchedule().stream().filter(e -> e.tier() == TourTier.DEVELOPMENT)
                .map(ScheduledTournament::courseIndex).toList()).doesNotHaveDuplicates();
        assertThat(world.currentSchedule().stream().filter(e -> e.tier() == TourTier.PRO)
                .map(ScheduledTournament::catalogueId).toList()).containsOnlyNulls();
    }

    @Test
    void missingCatalogueProvenanceKeepsLegacyNamesAndFutureAllocation() {
        World original = World.create(73L, SMALL_STANDARD);
        WorldSnapshot snapshot = original.snapshot();
        List<ScheduledTournament> oldSchedule = snapshot.schedule().stream().map(event ->
                event.tier() == TourTier.PRO ? event : new ScheduledTournament(event.week(), event.tier(),
                        event.prestige() == EventPrestige.TOUR_CHAMPIONSHIP ? 0
                                : (int) (event.tournamentId() % SMALL_STANDARD.coursePoolSize()),
                        event.prestige(), event.tournamentId(), event.pinPlacementVersion(), event.puttingPolicy()))
                .toList();
        WorldSnapshot legacySnapshot = new WorldSnapshot(snapshot.season(), snapshot.week(),
                snapshot.nextTournamentId(), snapshot.replenishCounter(), snapshot.previousNumberOne(),
                snapshot.golfers(), snapshot.careers(), snapshot.activeGolfers(), snapshot.accounts(),
                snapshot.physicalStates(), snapshot.supportTeams(), snapshot.equipment(), snapshot.loadouts(),
                snapshot.tours(), snapshot.ranking(), snapshot.statistics(), snapshot.media(),
                snapshot.hallOfFameInductions(), snapshot.hallOfFameMembers(), snapshot.retirementSeason(),
                snapshot.archives(), snapshot.rankingSnapshots(), snapshot.environmentalHistory(),
                snapshot.healthHistory(), snapshot.seasonResults(), oldSchedule, snapshot.announcedProspects(),
                snapshot.playerControl(), snapshot.playerPendingOffers(), snapshot.playerPendingStaff(),
                snapshot.playerPendingEquipment(), snapshot.achievedGoals(), snapshot.staffPool(),
                snapshot.playerActiveEquipmentDeal(), snapshot.playerPendingEquipmentDeals(),
                snapshot.unlockedAchievements(), snapshot.majorsWonThisSeason(), snapshot.playerCareerRecords(),
                snapshot.courseGeneratorVersion(), snapshot.defaultPinPlacementVersion(), snapshot.coursePoolPolicy(),
                snapshot.careerPreparation(), null);
        World legacy = World.restore(73L, SMALL_STANDARD, legacySnapshot);
        assertThat(legacy.developmentCatalogueVersion()).isNull();
        assertThat(legacy.currentSchedule().stream().filter(e -> e.tier() == TourTier.PRO).toList())
                .isEqualTo(original.currentSchedule().stream().filter(e -> e.tier() == TourTier.PRO).toList());
        var legacyRegular = legacy.currentSchedule().stream()
                .filter(e -> e.tier() == TourTier.DEVELOPMENT && e.prestige() == EventPrestige.REGULAR)
                .findFirst().orElseThrow();
        assertThat(legacy.nameFor(legacyRegular)).startsWith("The ").endsWith(" Open");
        legacy.advanceSeason();
        assertThat(legacy.currentSchedule().stream().filter(e -> e.tier() == TourTier.DEVELOPMENT)
                .map(ScheduledTournament::catalogueId).toList()).containsOnlyNulls();
        assertThat(legacy.currentSchedule().stream()
                .filter(e -> e.tier() == TourTier.DEVELOPMENT && e.prestige() == EventPrestige.REGULAR)
                .allMatch(e -> e.courseIndex() == e.tournamentId() % SMALL_STANDARD.coursePoolSize())).isTrue();
    }
}
