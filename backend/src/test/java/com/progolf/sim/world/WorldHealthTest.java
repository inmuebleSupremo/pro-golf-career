package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.health.HealthEvent;
import com.progolf.sim.health.PhysicalState;
import java.util.List;
import org.junit.jupiter.api.Test;

/** world-progression (modified): physical state gates fields, accrues/recovers, records health; reproducible. */
class WorldHealthTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void everyGolferStartsHealthyAndAvailable() {
        World world = World.create(101L, small());
        for (String id : world.activeGolferIds()) {
            PhysicalState state = world.physicalStateOf(id);
            assertThat(state).isNotNull();
            assertThat(state.canCompete()).isTrue();
            assertThat(state.fatigue()).isZero();
        }
    }

    @Test
    void competingAccruesFatigue() {
        World world = World.create(202L, small());
        world.advanceWeek(); // week 1 resolves an event per tier

        boolean anyFatigued = world.activeGolferIds().stream()
                .map(world::physicalStateOf)
                .anyMatch(s -> s.fatigue() > 0.0);
        assertThat(anyFatigued).as("competitors accrued fatigue").isTrue();
    }

    @Test
    void significantHealthEventsAreRecordedOverTime() {
        World world = World.create(303L, small());
        world.advanceSeason();
        world.advanceSeason();
        world.advanceSeason();

        // Across hundreds of participations, significant injuries (and comebacks) accumulate (REQ-219/223).
        assertThat(world.healthHistory()).isNotEmpty();
        assertThat(world.healthHistory()).allSatisfy(e -> assertThat(e.summary()).isNotBlank());
    }

    @Test
    void physicalStateIsReproducibleFromTheSeed() {
        World a = World.create(404L, small());
        World b = World.create(404L, small());
        a.advanceSeason();
        a.advanceSeason();
        b.advanceSeason();
        b.advanceSeason();

        assertThat(healthSummary(a)).isEqualTo(healthSummary(b));
        assertThat(a.healthHistory()).isEqualTo(b.healthHistory());
    }

    private static List<String> healthSummary(World w) {
        return w.activeGolferIds().stream().sorted()
                .map(id -> {
                    PhysicalState s = w.physicalStateOf(id);
                    return id + "|" + s.fitness() + "|" + s.fatigue() + "|" + s.availability();
                })
                .toList();
    }
}
