package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.ShotGuidance;
import com.progolf.sim.shot.StrategicTargetPlanner;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.shot.StrategyPolicy;
import com.progolf.sim.spatial.Surface;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Fixed, unbiased Parkland/Links corpus for the retained Generator 8 strategic architecture. */
class PrincipledArchitecturePreviewTest {
    private static final long WORLD = 0x5052494e4349504cL;
    private static final long[] CORPUS = {103L, 271L, 509L, 887L, 1597L, 2861L};
    private static final Attributes PLAYER = Attributes.uniform(90);

    @Test
    void generatorEightIsDeterministicCanonicalAndLeavesRetainedGeneratorSevenUntouched() {
        for (EnvironmentClassification environment : new EnvironmentClassification[] {
                EnvironmentClassification.PARKLAND, EnvironmentClassification.LINKS}) {
            for (long seed : CORPUS) {
                SeedCoordinate coordinate = coordinate(seed, environment);
                Course preview = CourseGenerator.generatePrincipledArchitecturePreview(coordinate, environment);
                assertThat(CourseGenerator.generatePrincipledArchitecturePreview(coordinate, environment)).isEqualTo(preview);
                assertThat(CourseGenerator.generate(coordinate, environment,
                        CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION)).isEqualTo(preview);
                assertThat(preview.generatorVersion())
                        .isEqualTo(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
                assertThat(CourseGenerator.generate(coordinate, environment, CourseGenConstants.V5_REFINED_GENERATOR_VERSION)
                        .generatorVersion()).isEqualTo(CourseGenConstants.V5_REFINED_GENERATOR_VERSION);
                for (GeneratedHole hole : preview.holes()) {
                    PinPlacement pin = hole.pinPlacementFor(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
                    assertThat(hole.geometry().surfaceAt(pin.cup())).isEqualTo(Surface.GREEN);
                    List<TerrainRegion> hazards = new ArrayList<>();
                    for (TerrainRegion region : hole.geometry().regions()) {
                        if (region.surface() == Surface.BUNKER || region.surface() == Surface.WATER
                                || region.surface() == Surface.TREES || region.surface() == Surface.WASTE_AREA) {
                            assertThat(V5GeometryGenerator.hazardsHaveRoom(region, hazards)).isTrue();
                            hazards.add(region);
                        }
                    }
                }
            }
        }
    }

    @Test
    void completeFixedCorpusContainsVarietyAndRealSharedStrategicChoicesWhereGeometrySupportsThem() {
        int decisionHoles = 0;
        int parThree = 0, parFour = 0, parFive = 0;
        java.util.Set<Long> signatures = new java.util.HashSet<>();
        List<PreviewCourseMetrics> parklandMeasures = new ArrayList<>();
        List<PreviewCourseMetrics> linksMeasures = new ArrayList<>();
        for (EnvironmentClassification environment : new EnvironmentClassification[] {
                EnvironmentClassification.PARKLAND, EnvironmentClassification.LINKS}) {
            for (long seed : CORPUS) {
                Course course = CourseGenerator.generatePrincipledArchitecturePreview(coordinate(seed, environment), environment);
                PreviewCourseMetrics measures = PreviewCourseMetrics.inspect(course);
                (environment == EnvironmentClassification.PARKLAND ? parklandMeasures : linksMeasures).add(measures);
                for (GeneratedHole hole : course.holes()) {
                    if (hole.par() == 3) parThree++;
                    else if (hole.par() == 4) parFour++;
                    else parFive++;
                    signatures.add(Math.round(hole.architecturePlan().structuralSignature() * 1_000));
                    var model = hole.forRound(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
                    assertThat(model.architectureStrategyPlan()).isEqualTo(hole.architectureStrategyPlan());
                    List<ShotGuidance.StrategicOption> options = StrategicTargetPlanner.options(model,
                            new BallState(model.geometry().tee(), Surface.TEE_BOX), model.startDistance(), Surface.TEE_BOX,
                            PLAYER, Environment.calm());
                    if (options.size() > 1) {
                        decisionHoles++;
                        assertThat(options).anyMatch(option -> option.role() == LandingZoneRole.PRIMARY);
                        options.forEach(option -> assertThat(model.geometry().surfaceAt(new Position2d(
                                option.aimPoint().x(), option.aimPoint().y()))).isIn(Surface.FAIRWAY,
                                Surface.FIRST_CUT, Surface.PRIMARY_ROUGH));
                        for (Strategy strategy : Strategy.values()) {
                            ShotGuidance.StrategicOption preferred = StrategicTargetPlanner.preferred(options, strategy);
                            assertThat(((BallStrikeIntent) new StrategyPolicy(strategy).decideShotIntent(model,
                                    new BallState(model.geometry().tee(), Surface.TEE_BOX), model.startDistance(),
                                    Surface.TEE_BOX, PLAYER, model.par(), Environment.calm())).aimPoint())
                                    .isEqualTo(preferred.aimPoint());
                        }
                    }
                }
            }
        }
        assertThat(parThree).isPositive();
        assertThat(parFour).isPositive();
        assertThat(parFive).isPositive();
        assertThat(signatures).hasSizeGreaterThan(80);
        assertThat(decisionHoles).isPositive();
        List<PreviewCourseMetrics> measures = new ArrayList<>();
        measures.addAll(parklandMeasures);
        measures.addAll(linksMeasures);
        // A whole-course average is a guard against corridor-heavy output, not a fixed per-course routing quota.
        assertThat(measures.stream().mapToInt(PreviewCourseMetrics::straightforwardParFourFive).average().orElseThrow())
                .isLessThanOrEqualTo(4.25);
        assertThat(measures.stream().mapToInt(PreviewCourseMetrics::meaningfulTurnHoles).average().orElseThrow())
                .isGreaterThanOrEqualTo(9.0);
        assertThat(measures).allSatisfy(measure -> {
            assertThat(measure.minimumFairwayWidth()).isGreaterThanOrEqualTo(34.0);
            assertThat(measure.meanFairwayWidth()).isBetween(38.0, 56.0);
            assertThat(measure.groupedBunkerHoles()).isPositive();
        });
        assertThat(parklandMeasures.stream().mapToInt(PreviewCourseMetrics::treeRegions).average().orElseThrow())
                .isGreaterThan(linksMeasures.stream().mapToInt(PreviewCourseMetrics::treeRegions).average().orElseThrow() * 10.0);
    }

    @Test
    void completeFixedPreviewCorpusRemainsBounded() {
        assertTimeout(Duration.ofSeconds(5), () -> {
            for (EnvironmentClassification environment : new EnvironmentClassification[] {
                    EnvironmentClassification.PARKLAND, EnvironmentClassification.LINKS}) {
                for (long seed : CORPUS) {
                    assertThat(CourseGenerator.generatePrincipledArchitecturePreview(coordinate(seed, environment), environment)
                            .holes()).hasSize(18);
                }
            }
        });
    }

    private static SeedCoordinate coordinate(long seed, EnvironmentClassification environment) {
        return new SeedCoordinate(WORLD, 7, seed, environment.ordinal(), 0, 0, 0);
    }
}
