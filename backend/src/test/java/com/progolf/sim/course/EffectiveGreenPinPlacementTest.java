package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tournament.SetupDifficulty;
import com.progolf.sim.tournament.Tier;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Fixed corpus proving V5 cups use the canonical setup-specific GREEN rather than nominal dimensions. */
class EffectiveGreenPinPlacementTest {

    @Test
    void v5CupsAreGreenAndTwoYardsFromEveryEdgeAcrossSupportedSetups() {
        for (int generatorVersion = CourseGenConstants.V1_GENERATOR_VERSION;
             generatorVersion <= CourseGenConstants.V5_GENERATOR_VERSION; generatorVersion++) {
            for (long seed = 1; seed <= 12; seed++) {
                Course course = CourseGenerator.generate(new SeedCoordinate(0x5eedL, 1, seed, 0, 0, 0, 0),
                        EnvironmentClassification.values()[(int) (seed % EnvironmentClassification.values().length)],
                        generatorVersion);
                for (Tier tier : Tier.values()) for (EventPrestige prestige : EventPrestige.values()) {
                    CourseSetup setup = SetupDifficulty.forEvent(tier, prestige);
                    for (GeneratedHole hole : course.holes()) for (int round = 1; round <= 4; round++) {
                        HoleModel model = hole.forRound(round, setup, PinPlacementVersion.V5_EFFECTIVE_GREEN);
                        assertThat(model.geometry().surfaceAt(model.cupPosition()))
                                .as("v%d seed %d %s %s hole %d round %d", generatorVersion, seed, tier, prestige,
                                        hole.number(), round)
                                .isEqualTo(Surface.GREEN);
                        assertThat(GreenPinGeometry.clearanceToGreenBoundary(model.geometry(), model.cupPosition()))
                                .isGreaterThanOrEqualTo(GreenPinGeometry.CLEARANCE_YARDS);
                    }
                }
            }
        }
    }

    @Test
    void v5IsDeterministicAndLeavesLegacyCupDerivationUntouched() {
        Course course = CourseGenerator.generate(new SeedCoordinate(91L, 1, 4, 0, 0, 0, 0),
                EnvironmentClassification.LINKS, CourseGenConstants.V4_GENERATOR_VERSION);
        CourseSetup hardMajor = SetupDifficulty.forEvent(Tier.ELITE, EventPrestige.MAJOR);
        GeneratedHole hole = course.holes().get(0);

        HoleModel first = hole.forRound(3, hardMajor, PinPlacementVersion.V5_EFFECTIVE_GREEN);
        HoleModel second = hole.forRound(3, hardMajor, PinPlacementVersion.V5_EFFECTIVE_GREEN);
        assertThat(first.cupPosition()).isEqualTo(second.cupPosition());
        assertThat(hole.pinPlacementFor(3, hardMajor, PinPlacementVersion.V5_EFFECTIVE_GREEN))
                .isEqualTo(hole.pinPlacementFor(3, hardMajor, PinPlacementVersion.V5_EFFECTIVE_GREEN));

        HoleModel legacy = hole.forRound(3, hardMajor, PinPlacementVersion.LEGACY_V1);
        PinPosition historical = hole.pinFor(3, hardMajor);
        assertThat(legacy.cupPosition()).isEqualTo(new Position2d(legacy.geometry().greenCenter().x()
                + historical.lateralOffset(), legacy.geometry().greenCenter().y() + historical.depthOffset()));
        assertThat(course.generatorVersion()).isEqualTo(CourseGenConstants.V4_GENERATOR_VERSION);
    }

    @Test
    void unsupportedInsetFailsDeterministicallyInsteadOfReducingClearance() {
        List<Position2d> square = List.of(new Position2d(-1, -1), new Position2d(1, -1),
                new Position2d(1, 1), new Position2d(-1, 1));
        CourseGeometry geometry = new CourseGeometry(new Position2d(0, -10), new Position2d(0, 0),
                List.of(new Position2d(-20, -20), new Position2d(20, -20), new Position2d(20, 20),
                        new Position2d(-20, 20)), List.of(new TerrainRegion(Surface.GREEN, square)));
        assertThatThrownBy(() -> GreenPinGeometry.projectIntoEligibleGreen(geometry, new Position2d(0, 0),
                new Position2d(0.5, 0.5))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no eligible");
    }

    @Test
    void clearanceUsesTheNearestEdgeRatherThanDistanceFromTheGreenCentreOrVertices() {
        List<Position2d> square = List.of(new Position2d(-10, -10), new Position2d(10, -10),
                new Position2d(10, 10), new Position2d(-10, 10));
        CourseGeometry geometry = new CourseGeometry(new Position2d(0, -20), new Position2d(0, 0),
                List.of(new Position2d(-30, -30), new Position2d(30, -30), new Position2d(30, 30),
                        new Position2d(-30, 30)), List.of(new TerrainRegion(Surface.GREEN, square)));
        Position2d nearCorner = new Position2d(8.5, 8.5);

        // The nearest vertex is more than two yards away, but the nearest edges are only 1.5 yards away.
        assertThat(nearCorner.distanceTo(new Position2d(10, 10))).isGreaterThan(GreenPinGeometry.CLEARANCE_YARDS);
        assertThat(GreenPinGeometry.clearanceToGreenBoundary(geometry, nearCorner)).isEqualTo(1.5);
        assertThat(GreenPinGeometry.eligible(geometry, nearCorner)).isFalse();
    }

    @Test
    void projectionRetainsTwoSidedDepthAndLateralVarietyAndHardPinsReachTheInsetMoreOften() {
        int front = 0, back = 0, left = 0, right = 0, regularBoundary = 0, hardBoundary = 0;
        CourseSetup regular = SetupDifficulty.forEvent(Tier.DEVELOPMENT, EventPrestige.REGULAR);
        CourseSetup hard = SetupDifficulty.forEvent(Tier.ELITE, EventPrestige.MAJOR);
        for (long seed = 1; seed <= 12; seed++) {
            Course course = CourseGenerator.generate(new SeedCoordinate(0x51deL, 1, seed, 0, 0, 0, 0),
                    EnvironmentClassification.PARKLAND, CourseGenConstants.V4_GENERATOR_VERSION);
            for (GeneratedHole hole : course.holes()) for (int round = 1; round <= 4; round++) {
                PinPlacement easy = hole.pinPlacementFor(round, regular, PinPlacementVersion.V5_EFFECTIVE_GREEN);
                PinPlacement tough = hole.pinPlacementFor(round, hard, PinPlacementVersion.V5_EFFECTIVE_GREEN);
                if (tough.pin().depthOffset() < 0) front++; else if (tough.pin().depthOffset() > 0) back++;
                if (tough.pin().lateralOffset() < 0) left++; else if (tough.pin().lateralOffset() > 0) right++;
                if (GreenPinGeometry.clearanceToGreenBoundary(hole.geometryForWidth(regular.widthScale()), easy.cup())
                        <= GreenPinGeometry.CLEARANCE_YARDS + 1.0e-6) regularBoundary++;
                if (GreenPinGeometry.clearanceToGreenBoundary(hole.geometryForWidth(hard.widthScale()), tough.cup())
                        <= GreenPinGeometry.CLEARANCE_YARDS + 1.0e-6) hardBoundary++;
            }
        }
        assertThat(front).isPositive();
        assertThat(back).isPositive();
        assertThat(left).isPositive();
        assertThat(right).isPositive();
        assertThat(hardBoundary).isGreaterThan(regularBoundary);
    }
}
