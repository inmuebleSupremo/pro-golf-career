package com.progolf.app.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.app.api.dto.AimPointInput;
import com.progolf.app.api.dto.PhysicalPuttIntentInput;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.CourseSetup;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.course.PinPlacementVersion;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.spatial.Surface;
import org.junit.jupiter.api.Test;

class PhysicalPuttingApiMapperTest {
    @Test
    void physicalGreenProjectionSamplesOnlyTheResolverOwnedSurfaceAndNormalizesPlayerLine() {
        Course course = CourseGenerator.generate(new SeedCoordinate(91L, 2, 3, 4, 5, 6, 0), EnvironmentClassification.PARKLAND);
        var model = course.holeModel(7, 1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN, PuttingPolicy.PHYSICAL_V1);
        var hole = course.holes().get(6);
        var dto = ApiMapper.playingHole(hole, hole.pinPlacementFor(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN).pin(),
                "PARKLAND", "PARKLAND", 8, new BallState(model.geometry().tee(), Surface.TEE_BOX), model.geometry(),
                model.cupPosition(), new com.progolf.sim.shot.WindVector(0, 0), PuttingPolicy.PHYSICAL_V1, model.greenSurface());

        assertThat(dto.puttingPolicy()).isEqualTo("PHYSICAL_V1");
        assertThat(dto.physicalGreen().slopeSamples()).hasSizeGreaterThan(100);
        assertThat(dto.physicalGreen().slopeSamples()).allSatisfy(sample ->
                assertThat(model.greenSurface().contains(new com.progolf.sim.course.Position2d(sample.position().x(), sample.position().y()))).isTrue());
        assertThat(dto.physicalGreen().contours()).isNotEmpty();
        assertThat(dto.physicalGreen().contours()).allSatisfy(contour -> {
            assertThat(contour.points()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(contour.points()).allSatisfy(point -> assertThat(model.greenSurface()
                    .contains(new com.progolf.sim.course.Position2d(point.x(), point.y()))).isTrue());
        });
        assertThat(ApiMapper.physicalPuttIntent(new PhysicalPuttIntentInput(new AimPointInput(3, 4), 12, "r")).direction())
                .isEqualTo(new com.progolf.sim.course.Position2d(0.6, 0.8));
    }

    @Test
    void physicalInputRejectsMissingLineAndInvalidPace() {
        assertThatThrownBy(() -> ApiMapper.physicalPuttIntent(new PhysicalPuttIntentInput(null, 5, "r")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApiMapper.physicalPuttIntent(new PhysicalPuttIntentInput(new AimPointInput(0, 0), 5, "r")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApiMapper.physicalPuttIntent(new PhysicalPuttIntentInput(new AimPointInput(1, 0), 0, "r")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
