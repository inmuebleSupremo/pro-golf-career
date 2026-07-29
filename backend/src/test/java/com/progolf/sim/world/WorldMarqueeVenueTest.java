package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * world-schedule (permanent venues): marquee named events — the majors and the per-tour Tour Championships —
 * keep a permanent venue across seasons, derived deterministically from the world seed, while regular events
 * may still rotate. A small standard-cadence world (30 weeks, 4 majors) is used so a season advance is cheap.
 */
class WorldMarqueeVenueTest {

    /** Standard cadence (structured schedule with majors + championships) at a small scale for a fast advance. */
    private static WorldConfig structured() {
        return new WorldConfig(60, 30, 4, 24, 6, 4, 1);
    }

    private static Map<Integer, Integer> majorVenuesByWeek(List<ScheduledTournament> schedule) {
        Map<Integer, Integer> venues = new HashMap<>();
        for (ScheduledTournament s : schedule) {
            if (s.prestige() == EventPrestige.MAJOR) {
                venues.put(s.week(), s.courseIndex());
            }
        }
        return venues;
    }

    private static Map<TourTier, Integer> championshipVenuesByTier(List<ScheduledTournament> schedule) {
        Map<TourTier, Integer> venues = new EnumMap<>(TourTier.class);
        for (ScheduledTournament s : schedule) {
            if (s.prestige() == EventPrestige.TOUR_CHAMPIONSHIP) {
                venues.put(s.tier(), s.courseIndex());
            }
        }
        return venues;
    }

    @Test
    void majorsAndChampionshipsKeepTheirVenueAcrossSeasons() {
        World world = World.create(2026L, structured());

        Map<Integer, Integer> majorsSeason1 = majorVenuesByWeek(world.currentSchedule());
        Map<TourTier, Integer> championshipsSeason1 = championshipVenuesByTier(world.currentSchedule());
        assertThat(majorsSeason1).as("four majors at fixed weeks").hasSize(4);
        assertThat(championshipsSeason1).as("a championship per tour").isNotEmpty();

        world.advanceSeason();

        // The same major (identified by its fixed week) and the same tour's championship return to the same course.
        assertThat(majorVenuesByWeek(world.currentSchedule())).isEqualTo(majorsSeason1);
        assertThat(championshipVenuesByTier(world.currentSchedule())).isEqualTo(championshipsSeason1);
    }

    @Test
    void marqueeVenuesAreReproducibleAcrossSameSeedWorlds() {
        World a = World.create(99L, structured());
        World b = World.create(99L, structured());

        assertThat(majorVenuesByWeek(b.currentSchedule())).isEqualTo(majorVenuesByWeek(a.currentSchedule()));
        assertThat(championshipVenuesByTier(b.currentSchedule()))
                .isEqualTo(championshipVenuesByTier(a.currentSchedule()));
    }
}
