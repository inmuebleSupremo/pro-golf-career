package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tournament.SetupDifficulty;
import com.progolf.sim.tournament.Tier;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Guards against canonical terrain becoming visually rich but mechanically unreachable during migration. */
class CanonicalTerrainCompositionTest {

    @Test
    void canonicalPopulationRetainsMeaningfulMissAndHazardExposure() {
        Course course = CourseGenerator.generate(new SeedCoordinate(4242L, 3, 0, 0, 0, 0, 0),
                EnvironmentClassification.PARKLAND);
        CourseSetup setup = SetupDifficulty.forEvent(Tier.PREMIER, EventPrestige.REGULAR);
        Map<Surface, Integer> surfaces = new EnumMap<>(Surface.class);
        int penalties = 0;

        for (int round = 0; round < 200; round++) for (int hole = 1; hole <= 18; hole++) {
            RoundOutcome outcome = RoundResolver.resolveHole(course.holeModel(hole, 1, setup),
                    Attributes.uniform(88), new GolferState(0.35, 0, 0, 0, 0, 0, 0, 0, 0), Environment.calm(),
                    Strategy.AGGRESSIVE, new SeedCoordinate(999L, 2, 88, round, hole, 9, 0));
            for (var shot : outcome.shots()) {
                surfaces.merge(shot.contactSurface(), 1, Integer::sum);
                penalties += shot.penaltyStrokes();
            }
        }

        int bunker = surfaces.getOrDefault(Surface.BUNKER, 0);
        int flankHazards = surfaces.getOrDefault(Surface.WATER, 0) + surfaces.getOrDefault(Surface.TREES, 0);
        int outOfBounds = surfaces.getOrDefault(Surface.OUT_OF_BOUNDS, 0);
        int green = surfaces.getOrDefault(Surface.GREEN, 0);
        int fringe = surfaces.getOrDefault(Surface.FRINGE, 0);

        // Broad migration floors, not exact copies of the legacy zone-band counts. They catch a
        // decorative/unreachable hazard layout, an indefinitely safe boundary, or a fringe collar
        // that swallows the playable green while leaving future course-design freedom intact.
        assertThat(bunker).as("greenside bunker exposure").isGreaterThanOrEqualTo(100);
        assertThat(flankHazards).as("water/tree flank exposure").isGreaterThanOrEqualTo(5);
        assertThat(outOfBounds).as("playable-boundary exposure").isGreaterThanOrEqualTo(5);
        assertThat(penalties).as("hazard and boundary penalties").isGreaterThanOrEqualTo(10);
        assertThat(green).as("green must remain materially more reachable than fringe").isGreaterThan(fringe * 8);
    }
}
