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
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Fixed resolved-outcome check: V4 aggressive risk is measurable without making the safe route unusable. */
class RoleBasedHazardOutcomeTest {
    private static final long WORLD = 0x56345249534b4f55L;

    @Test
    void aggressiveRiskRewardResolutionCarriesBoundedAdditionalHazardExposure() {
        GeneratedHole hole = IntStream.range(0, 500).mapToObj(id -> CourseGenerator.generate(
                        new SeedCoordinate(WORLD, 5, id, 0, 0, 0, 0), EnvironmentClassification.COASTAL,
                        CourseGenConstants.V4_GENERATOR_VERSION))
                .flatMap(course -> course.holes().stream())
                .filter(candidate -> candidate.spatialPlan().landingZones().stream()
                        .anyMatch(zone -> zone.role() == LandingZoneRole.AGGRESSIVE))
                .filter(candidate -> !candidate.spatialPlan().route().intermediateAnchors().isEmpty())
                .filter(candidate -> candidate.hazardPlan().features().stream().anyMatch(feature -> feature.role()
                        == HazardRole.LANDING_GUARD && feature.anchor().landingZoneRole() == LandingZoneRole.AGGRESSIVE))
                .findFirst().orElseThrow();

        int conservativeHazards = 0;
        int aggressiveHazards = 0;
        int conservativeStrokes = 0;
        int aggressiveStrokes = 0;
        for (int sample = 0; sample < 96; sample++) {
            SeedCoordinate coordinate = new SeedCoordinate(WORLD, 5, 900, sample, 0, hole.number(), 0);
            RoundOutcome safe = RoundResolver.resolveHole(hole.forRound(1), Attributes.uniform(72), GolferState.fresh(),
                    Environment.calm(), Strategy.CONSERVATIVE, coordinate);
            RoundOutcome aggressive = RoundResolver.resolveHole(hole.forRound(1), Attributes.uniform(72), GolferState.fresh(),
                    Environment.calm(), Strategy.AGGRESSIVE, coordinate);
            conservativeHazards += contacts(safe);
            aggressiveHazards += contacts(aggressive);
            conservativeStrokes += safe.totalStrokes();
            aggressiveStrokes += aggressive.totalStrokes();
            assertThat(safe.shots()).hasSizeLessThan(20);
            assertThat(aggressive.shots()).hasSizeLessThan(20);
        }
        System.out.printf("[role-based-risk-outcomes] safeHazards=%d aggressiveHazards=%d safeStrokes=%d aggressiveStrokes=%d%n",
                conservativeHazards, aggressiveHazards, conservativeStrokes, aggressiveStrokes);
        assertThat(aggressiveHazards).isGreaterThanOrEqualTo(conservativeHazards);
        assertThat(aggressiveStrokes).isLessThanOrEqualTo(conservativeStrokes + 96 * 3);
    }

    private static int contacts(RoundOutcome outcome) {
        return (int) outcome.shots().stream().filter(shot -> shot.finalSurface() == Surface.BUNKER
                || shot.finalSurface() == Surface.WATER || shot.finalSurface() == Surface.TREES
                || shot.finalSurface() == Surface.RECOVERY_AREA).count();
    }
}
