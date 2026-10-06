package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CourseDesignTest {
    private static final long WORLD = 0xBEEF_CAFE_1234_5678L;

    @Test
    void v1RemainsExplicitlyReproducibleAndV2CarriesAnInternalPlan() {
        SeedCoordinate coordinate = new SeedCoordinate(WORLD, 1, 7, 0, 0, 0, 0);
        Course v1 = CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                CourseGenConstants.V1_GENERATOR_VERSION);
        Course v2 = CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                CourseGenConstants.V2_GENERATOR_VERSION);

        assertThat(CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                CourseGenConstants.V1_GENERATOR_VERSION)).isEqualTo(v1);
        assertThat(v1.designProfile()).isNull();
        assertThat(v1.coursePlan()).isNull();
        assertThat(v2.generatorVersion()).isEqualTo(CourseGenConstants.V2_GENERATOR_VERSION);
        assertThat(v2.designProfile()).isNotNull();
        assertThat(v2.coursePlan().briefs()).hasSize(18);
        assertThat(v2.totalPar()).isEqualTo(72);
        assertThat(v2.holes()).extracting(GeneratedHole::par).containsExactlyElementsOf(
                v2.coursePlan().briefs().stream().map(HoleBrief::par).toList());
    }

    @Test
    void allProfileCombinationsCreateValidDeterministicPlans() {
        for (StrategicEmphasis strategic : StrategicEmphasis.values()) {
            for (WidthTendency width : WidthTendency.values()) {
                for (RecoverySeverity recovery : RecoverySeverity.values()) {
                    CourseDesignProfile profile = new CourseDesignProfile(strategic, width, recovery);
                    CoursePlan first = CoursePlanGenerator.generate(42L, profile);
                    CoursePlan second = CoursePlanGenerator.generate(42L, profile);
                    assertThat(first).isEqualTo(second);
                    assertThat(first.briefs()).hasSize(18);
                    assertThat(first.briefs().stream().map(HoleBrief::par).mapToInt(Integer::intValue).sum()).isEqualTo(72);
                }
            }
        }
    }

    @Test
    void fixedFortyEightCoordinateCorpusPreservesV1AndValidatesEveryV2Profile() {
        for (EnvironmentClassification classification : EnvironmentClassification.values()) {
            for (int coordinateId = 0; coordinateId < 8; coordinateId++) {
                SeedCoordinate coordinate = new SeedCoordinate(WORLD, 1,
                        classification.ordinal() * 8L + coordinateId, 0, 0, 0, 0);
                Course v1 = CourseGenerator.generate(coordinate, classification, CourseGenConstants.V1_GENERATOR_VERSION);
                assertThat(CourseGenerator.generate(coordinate, classification, CourseGenConstants.V1_GENERATOR_VERSION))
                        .isEqualTo(v1);
                for (StrategicEmphasis strategic : StrategicEmphasis.values()) {
                    for (WidthTendency width : WidthTendency.values()) {
                        for (RecoverySeverity recovery : RecoverySeverity.values()) {
                            CourseDesignProfile profile = new CourseDesignProfile(strategic, width, recovery);
                            Course v2 = CourseGenerator.generateV2(coordinate, classification, profile);
                            assertThat(v2.designProfile()).isEqualTo(profile);
                            assertThat(v2.coursePlan().briefs()).hasSize(18);
                            assertThat(v2.holes()).allSatisfy(hole -> {
                                assertThat(hole.geometry().surfaceAt(hole.geometry().tee())).isNotEqualTo(Surface.OUT_OF_BOUNDS);
                                assertThat(hole.geometry().surfaceAt(hole.geometry().greenCenter())).isEqualTo(Surface.GREEN);
                            });
                        }
                    }
                }
            }
        }
    }

    @Test
    void profileAxesProduceOrderedWidthRecoveryAndStrategicDistributions() {
        CourseDesignProfile generous = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.GENEROUS,
                RecoverySeverity.BALANCED);
        CourseDesignProfile balanced = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.BALANCED,
                RecoverySeverity.BALANCED);
        CourseDesignProfile exacting = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.EXACTING,
                RecoverySeverity.BALANCED);
        assertThat(meanFairwayHalf(generous)).isGreaterThan(meanFairwayHalf(balanced));
        assertThat(meanFairwayHalf(balanced)).isGreaterThan(meanFairwayHalf(exacting));

        CourseDesignProfile forgiving = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.BALANCED,
                RecoverySeverity.FORGIVING);
        CourseDesignProfile penal = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.BALANCED,
                RecoverySeverity.PENAL);
        assertThat(recoveryExposure(forgiving)).isLessThan(recoveryExposure(balanced));
        assertThat(recoveryExposure(balanced)).isLessThan(recoveryExposure(penal));

        assertThat(archetypeCount(StrategicEmphasis.POSITIONAL, StrategicArchetype.POSITIONAL))
                .isGreaterThan(archetypeCount(StrategicEmphasis.RISK_REWARD, StrategicArchetype.POSITIONAL));
        assertThat(archetypeCount(StrategicEmphasis.RISK_REWARD, StrategicArchetype.RISK_REWARD))
                .isGreaterThan(archetypeCount(StrategicEmphasis.POSITIONAL, StrategicArchetype.RISK_REWARD));
    }

    @Test
    void unsupportedVersionFailsClearly() {
        assertThatThrownBy(() -> CourseGenerator.generate(new SeedCoordinate(1, 0, 0, 0, 0, 0, 0),
                EnvironmentClassification.PARKLAND, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported course generator version");
    }

    @Test
    void lockedV1BaselineAndV2CalibrationContract() {
        java.util.List<Course> v1 = v1Corpus();
        assertThat(sha256(v1.toString())).isEqualTo("7958abda6d8f2007f2da9d540d6b5d1108f2920f962d81044f0925a844c0aef4");
        assertThat(v1.stream().flatMap(course -> course.holes().stream()).mapToDouble(GeneratedHole::length).average().orElseThrow())
                .isEqualTo(381.4284300017833);
        assertThat(v1.stream().flatMap(course -> course.holes().stream()).mapToDouble(GeneratedHole::fairwayHalfWidth).average().orElseThrow())
                .isEqualTo(18.022297684634022);
        assertThat(v1.stream().flatMap(course -> course.holes().stream())
                .filter(h -> h.hasGreensideBunker() || h.hasWater() || h.hasTrees()).count()).isEqualTo(660);

        CourseDesignProfile generous = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.GENEROUS,
                RecoverySeverity.BALANCED);
        CourseDesignProfile balanced = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.BALANCED,
                RecoverySeverity.BALANCED);
        CourseDesignProfile exacting = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.EXACTING,
                RecoverySeverity.BALANCED);
        assertThat(meanFairwayHalf(generous) - meanFairwayHalf(balanced)).isGreaterThanOrEqualTo(2.0);
        assertThat(meanFairwayHalf(balanced) - meanFairwayHalf(exacting)).isGreaterThanOrEqualTo(2.0);
        CourseDesignProfile forgiving = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.BALANCED,
                RecoverySeverity.FORGIVING);
        CourseDesignProfile penal = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.BALANCED,
                RecoverySeverity.PENAL);
        assertThat(recoveryExposure(balanced) - recoveryExposure(forgiving)).isGreaterThanOrEqualTo(100);
        assertThat(recoveryExposure(penal) - recoveryExposure(balanced)).isGreaterThanOrEqualTo(100);
    }

    private static double meanFairwayHalf(CourseDesignProfile profile) {
        return corpus(profile).stream().flatMap(course -> course.holes().stream())
                .mapToDouble(GeneratedHole::fairwayHalfWidth).average().orElseThrow();
    }

    private static long recoveryExposure(CourseDesignProfile profile) {
        return corpus(profile).stream().flatMap(course -> course.holes().stream())
                .filter(hole -> hole.hasGreensideBunker() || hole.hasWater() || hole.hasTrees()).count();
    }

    private static long archetypeCount(StrategicEmphasis emphasis, StrategicArchetype archetype) {
        return corpus(new CourseDesignProfile(emphasis, WidthTendency.BALANCED, RecoverySeverity.BALANCED)).stream()
                .flatMap(course -> course.coursePlan().briefs().stream()).filter(brief -> brief.archetype() == archetype).count();
    }

    private static java.util.List<Course> corpus(CourseDesignProfile profile) {
        return Arrays.stream(EnvironmentClassification.values()).flatMap(classification -> java.util.stream.IntStream.range(0, 8)
                .mapToObj(id -> CourseGenerator.generateV2(new SeedCoordinate(WORLD, 1,
                        classification.ordinal() * 8L + id, 0, 0, 0, 0), classification, profile))).toList();
    }

    private static java.util.List<Course> v1Corpus() {
        return Arrays.stream(EnvironmentClassification.values()).flatMap(classification -> java.util.stream.IntStream.range(0, 8)
                .mapToObj(id -> CourseGenerator.generate(new SeedCoordinate(WORLD, 1,
                        classification.ordinal() * 8L + id, 0, 0, 0, 0), classification,
                        CourseGenConstants.V1_GENERATOR_VERSION))).toList();
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
