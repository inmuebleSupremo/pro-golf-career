package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.weather.EnvironmentalRecord;
import com.progolf.sim.weather.WeatherConstants;
import org.junit.jupiter.api.Test;

/** world-progression (modified): events play under generated weather; history is reproducible & significant. */
class WorldWeatherTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void everyRecordedEnvironmentIsActuallySignificant() {
        World world = World.create(7777L, small());
        world.advanceSeason();
        world.advanceSeason();

        // Only severe/record conditions are preserved (REQ-235).
        for (EnvironmentalRecord r : world.environmentalHistory()) {
            assertThat(r.severity()).isGreaterThanOrEqualTo(WeatherConstants.SEVERITY_THRESHOLD);
            assertThat(r.summary()).isNotBlank();
        }
    }

    @Test
    void weatherAndItsHistoryAreReproducibleFromTheSeed() {
        World a = World.create(4242L, small());
        World b = World.create(4242L, small());
        a.advanceSeason();
        a.advanceSeason();
        b.advanceSeason();
        b.advanceSeason();

        // Two worlds from the same seed generate identical weather, so their environmental history matches.
        assertThat(a.environmentalHistory()).isEqualTo(b.environmentalHistory());
    }
}
