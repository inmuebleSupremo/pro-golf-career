package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

/** world-schedule (realistic naming): events carry deterministic, flavourful display names. */
class WorldEventNamingTest {

    @Test
    void everyEventHasARealisticName() {
        World world = World.create(1L, WorldConfig.defaults());
        world.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);

        var schedule = world.playerSchedule();
        assertThat(schedule).isNotEmpty();
        assertThat(schedule).allSatisfy(e -> assertThat(e.name()).isNotBlank());

        // Majors carry the fixed, evocative fictional names.
        assertThat(schedule).filteredOn(e -> e.prestige() == EventPrestige.MAJOR)
                .isNotEmpty()
                .allSatisfy(e -> assertThat(Arrays.asList(EventNaming.MAJOR_NAMES)).contains(e.name()));

        // A regular event reads as "The <venue> Open"; a tour championship names its tour.
        assertThat(schedule).filteredOn(e -> e.prestige() == EventPrestige.REGULAR)
                .isNotEmpty()
                .anySatisfy(e -> assertThat(e.name()).startsWith("The ").endsWith(" Open"));
        assertThat(schedule).filteredOn(e -> e.prestige() == EventPrestige.TOUR_CHAMPIONSHIP)
                .allSatisfy(e -> assertThat(e.name()).endsWith("Tour Championship"));
    }

    @Test
    void everyEventHasALocationAndMajorsCarryTheirHintingPlace() {
        World world = World.create(1L, WorldConfig.defaults());
        world.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);
        var schedule = world.playerSchedule();
        // Every event is placed somewhere.
        assertThat(schedule).allSatisfy(e -> assertThat(e.location()).isNotBlank());

        // A major's location is its fixed hinting place.
        assertThat(schedule).filteredOn(e -> e.prestige() == EventPrestige.MAJOR)
                .isNotEmpty()
                .allSatisfy(e -> assertThat(Arrays.asList(EventNaming.MAJOR_LOCATIONS)).contains(e.location()));
    }

    @Test
    void theProTourIsAFixedCuratedCalendarIdenticalEverySeed() {
        // Read the whole schedule (not the player's tour-filtered view) so we see the Pro tour directly.
        var a = proNonAnchorLabels(3L);
        var b = proNonAnchorLabels(88L);

        // The Pro tour's non-major, non-championship events come entirely from the fixed curated list...
        var curated = Arrays.stream(EventNaming.PRO_EVENTS)
                .map(p -> p.name() + " | " + p.location()).toList();
        assertThat(a).isNotEmpty().allSatisfy(label ->
                assertThat(curated).contains(label.substring(label.indexOf(' ') + 1)));

        // ...and are identical across two different seeds (the point: a stable top-tour narrative).
        assertThat(a).isEqualTo(b);
    }

    @Test
    void theDevelopmentTourStaysProcedurallyNamedByVenue() {
        // The feeder tour keeps venue-derived names/regions (varies year to year), unlike the fixed Pro tour.
        World world = World.create(1L, WorldConfig.defaults());
        world.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);
        assertThat(world.playerSchedule())
                .filteredOn(e -> e.tier() == TourTier.DEVELOPMENT && e.prestige() == EventPrestige.REGULAR)
                .isNotEmpty()
                .allSatisfy(e -> assertThat(e.name()).startsWith("The ").endsWith(" Open"));
    }

    /** Season 1's Pro non-major/non-championship events as "week name | location", by week — via the schedule. */
    @SuppressWarnings("unchecked")
    private static List<String> proNonAnchorLabels(long seed) {
        World world = World.create(seed, WorldConfig.defaults());
        try {
            Field f = World.class.getDeclaredField("schedule");
            f.setAccessible(true);
            List<ScheduledTournament> schedule = (List<ScheduledTournament>) f.get(world);
            return schedule.stream()
                    .filter(e -> e.tier() == TourTier.PRO && e.prestige() != EventPrestige.MAJOR
                            && e.prestige() != EventPrestige.TOUR_CHAMPIONSHIP)
                    .sorted(Comparator.comparingInt(ScheduledTournament::week))
                    .map(e -> e.week() + " " + world.nameFor(e) + " | " + world.locationFor(e))
                    .toList();
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void namesAreDeterministicForTheSameSeed() {
        World a = World.create(7L, WorldConfig.defaults());
        World b = World.create(7L, WorldConfig.defaults());
        a.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);
        b.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);
        assertThat(a.playerSchedule().stream().map(PlayerScheduleEntry::name).toList())
                .isEqualTo(b.playerSchedule().stream().map(PlayerScheduleEntry::name).toList());
    }
}
