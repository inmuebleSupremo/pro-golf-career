package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.course.PinPlacementVersion;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DevelopmentTourHostNamingTest {
    private static final WorldConfig STANDARD = new WorldConfig(8, 30, 1, 4, 12, 4, 1);

    @Test
    void versionTwoNamesRespectAllFiveClassificationsAcrossSeeds() {
        long[] seeds = {0L, 1L, 2L, 47L, 201L, -271L, Long.MAX_VALUE};
        Set<EnvironmentClassification> expected = Set.of(EnvironmentClassification.LINKS,
                EnvironmentClassification.PARKLAND, EnvironmentClassification.WOODLAND,
                EnvironmentClassification.HEATHLAND, EnvironmentClassification.DESERT);
        assertThat(DevelopmentTourCatalogue.venues().stream().map(DevelopmentTourCatalogue.Venue::classification)
                .collect(Collectors.toSet())).isEqualTo(expected);
        int linksNames = 0;
        for (long seed : seeds) {
            World world = World.create(seed, STANDARD);
            assertThat(world.developmentCatalogueVersion()).isEqualTo(DevelopmentTourCatalogue.VERSION);
            for (int index = 0; index < DevelopmentTourCatalogue.venues().size(); index++) {
                var venue = DevelopmentTourCatalogue.venue(index);
                var host = world.hostIdentityFor(occurrence(venue, index, 1));
                assertThat(host.id()).isEqualTo(venue.id() + "-" + Long.toUnsignedString(seed, 16));
                assertThat(host.region()).isEqualTo(venue.region());
                assertThat(host.classification()).isEqualTo(venue.classification());
                assertThat(host.name()).isNotBlank();
                if (venue.classification() == EnvironmentClassification.LINKS) {
                    if (host.name().endsWith(" Links")) linksNames++;
                } else {
                    assertThat(host.name()).doesNotEndWith(" Links");
                }
            }
            assertThat(world.generatedDevelopmentCourseCount()).isZero();
        }
        assertThat(linksNames).isPositive();
    }

    @Test
    void versionOneNamesSurviveRestoreAndFutureHostSelections() {
        World current = World.create(47L, STANDARD);
        World original = withVersion(current, DevelopmentTourCatalogue.ORIGINAL_NAMING_VERSION);
        World restored = World.restore(47L, STANDARD, original.snapshot());
        assertThat(original.developmentCatalogueVersion()).isEqualTo(1);
        assertThat(restored.developmentCatalogueVersion()).isEqualTo(1);
        assertThat(original.currentSchedule()).isEqualTo(restored.currentSchedule());
        assertThat(original.currentSchedule()).isEqualTo(current.currentSchedule());

        var sandhills = scheduled(original, "sandhills-classic");
        var hudson = scheduled(original, "hudson-valley");
        assertThat(original.hostIdentityFor(sandhills).name()).isEqualTo("Pine Ridge Links");
        assertThat(original.hostIdentityFor(hudson).name()).isEqualTo("Royal Hollow Golf Links");
        assertThat(current.hostIdentityFor(sandhills).name()).isEqualTo("Pine Ridge Country Club");
        assertThat(current.hostIdentityFor(hudson).name()).isEqualTo("Royal Hollow Golf Club");

        for (int season = 1; season <= 6; season++) {
            for (var event : DevelopmentTourCatalogue.events()) {
                int index = DevelopmentTourCatalogue.selectedVenueIndex(event, 47L, season);
                var occurrence = occurrence(DevelopmentTourCatalogue.venue(index), index, season);
                assertThat(restored.hostIdentityFor(occurrence)).isEqualTo(original.hostIdentityFor(occurrence));
                assertThat(restored.nameFor(occurrence)).isEqualTo(event.name());
                assertThat(current.nameFor(occurrence)).isEqualTo(event.name());
            }
        }
    }

    @Test
    void namingVersionsKeepAssignmentsProPoolSeedsAndGeometry() {
        for (int generator : new int[] {CourseGenConstants.V5_REFINED_GENERATOR_VERSION,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION,
                CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION}) {
            World current = World.create(47L, STANDARD, generator);
            World original = withVersion(current, DevelopmentTourCatalogue.ORIGINAL_NAMING_VERSION);
            assertThat(original.currentSchedule()).isEqualTo(current.currentSchedule());
            assertThat(original.currentSchedule().stream().filter(e -> e.tier() == TourTier.PRO).toList())
                    .isEqualTo(current.currentSchedule().stream().filter(e -> e.tier() == TourTier.PRO).toList());
            for (int i = 0; i < STANDARD.coursePoolSize(); i++) {
                assertThat(original.courseAt(i)).isEqualTo(current.courseAt(i));
            }
            for (String eventId : List.of("hudson-valley", "sandhills-classic", "lowcountry-coastal")) {
                var event = scheduled(current, eventId);
                Course oldCourse = original.courseFor(event.courseIndex());
                Course newCourse = current.courseFor(event.courseIndex());
                assertThat(newCourse.identity().id()).isEqualTo(oldCourse.identity().id());
                assertThat(newCourse.identity().region()).isEqualTo(oldCourse.identity().region());
                assertThat(newCourse.identity().classification()).isEqualTo(oldCourse.identity().classification());
                assertThat(newCourse.identity().style()).isEqualTo(oldCourse.identity().style());
                assertThat(newCourse.holes()).isEqualTo(oldCourse.holes());
                assertThat(newCourse.coursePlan()).isEqualTo(oldCourse.coursePlan());
                assertThat(newCourse.architecturePlan()).isEqualTo(oldCourse.architecturePlan());
                assertThat(newCourse.generatorVersion()).isEqualTo(oldCourse.generatorVersion());
            }
            var links = scheduled(current, "lowcountry-coastal");
            assertThat(current.hostIdentityFor(links)).isEqualTo(original.hostIdentityFor(links));
            for (var event : current.currentSchedule()) {
                assertThat(current.nameFor(event)).isEqualTo(original.nameFor(event));
            }
        }
    }

    @Test
    void missingCatalogueVersionStillUsesTheOriginalSharedPoolNames() {
        World current = World.create(47L, STANDARD);
        List<ScheduledTournament> oldSchedule = current.currentSchedule().stream().map(event ->
                event.catalogueId() == null ? event : new ScheduledTournament(event.week(), event.tier(),
                        (int) (event.tournamentId() % STANDARD.coursePoolSize()), event.prestige(),
                        event.tournamentId(), event.pinPlacementVersion(), event.puttingPolicy())).toList();
        World legacy = withVersion(current, null, oldSchedule);
        assertThat(legacy.developmentCatalogueVersion()).isNull();
        for (int i = 0; i < STANDARD.coursePoolSize(); i++) {
            assertThat(legacy.courseAt(i)).isEqualTo(current.courseAt(i));
        }
        for (var event : oldSchedule) {
            if (event.tier() == TourTier.PRO) {
                assertThat(legacy.nameFor(event)).isEqualTo(current.nameFor(event));
            } else {
                assertThat(legacy.hostIdentityFor(event)).isNull();
                if (!event.prestige().isMajor() && event.prestige() != EventPrestige.TOUR_CHAMPIONSHIP) {
                    assertThat(legacy.nameFor(event)).contains(legacy.courseAt(event.courseIndex()).identity().name());
                }
            }
        }
    }

    private static ScheduledTournament scheduled(World world, String catalogueId) {
        return world.currentSchedule().stream().filter(e -> catalogueId.equals(e.catalogueId()))
                .findFirst().orElseThrow();
    }

    private static ScheduledTournament occurrence(DevelopmentTourCatalogue.Venue venue, int venueIndex, int season) {
        var event = DevelopmentTourCatalogue.forId(venue.eventId());
        return new ScheduledTournament(event.week(), TourTier.DEVELOPMENT, STANDARD.coursePoolSize() + venueIndex,
                event.prestige(), season, PinPlacementVersion.V5_EFFECTIVE_GREEN, PuttingPolicy.LEGACY, event.id());
    }

    private static World withVersion(World world, Integer version) {
        return withVersion(world, version, world.currentSchedule());
    }

    private static World withVersion(World world, Integer version, List<ScheduledTournament> schedule) {
        WorldSnapshot s = world.snapshot();
        WorldSnapshot pinned = new WorldSnapshot(s.season(), s.week(), s.nextTournamentId(), s.replenishCounter(),
                s.previousNumberOne(), s.golfers(), s.careers(), s.activeGolfers(), s.accounts(),
                s.physicalStates(), s.supportTeams(), s.equipment(), s.loadouts(), s.tours(), s.ranking(),
                s.statistics(), s.media(), s.hallOfFameInductions(), s.hallOfFameMembers(), s.retirementSeason(),
                s.archives(), s.rankingSnapshots(), s.environmentalHistory(), s.healthHistory(), s.seasonResults(),
                schedule, s.announcedProspects(), s.playerControl(), s.playerPendingOffers(),
                s.playerPendingStaff(), s.playerPendingEquipment(), s.achievedGoals(), s.staffPool(),
                s.playerActiveEquipmentDeal(), s.playerPendingEquipmentDeals(), s.unlockedAchievements(),
                s.majorsWonThisSeason(), s.playerCareerRecords(), s.courseGeneratorVersion(),
                s.defaultPinPlacementVersion(), s.coursePoolPolicy(), s.careerPreparation(), version);
        return World.restore(47L, STANDARD, pinned);
    }
}
