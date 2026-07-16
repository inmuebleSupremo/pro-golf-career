package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.tournament.EventPrestige;
import java.util.Arrays;
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
    void namesAreDeterministicForTheSameSeed() {
        World a = World.create(7L, WorldConfig.defaults());
        World b = World.create(7L, WorldConfig.defaults());
        a.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);
        b.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);
        assertThat(a.playerSchedule().stream().map(PlayerScheduleEntry::name).toList())
                .isEqualTo(b.playerSchedule().stream().map(PlayerScheduleEntry::name).toList());
    }
}
