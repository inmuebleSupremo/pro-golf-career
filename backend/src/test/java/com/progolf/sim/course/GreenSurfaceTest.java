package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GreenSurfaceTest {

    private static final List<Position2d> SQUARE = List.of(
            new Position2d(0, 0), new Position2d(2, 0), new Position2d(2, 2), new Position2d(0, 2));

    @Test
    void bilinearInterpolationReturnsTheExpectedElevationAndGradientForAnInclinedReferenceSurface() {
        GreenSurface surface = surface((x, y) -> 0.02 * x - 0.01 * y);

        GreenSurfaceSample sample = surface.sampleAt(new Position2d(0.75, 1.25));

        assertThat(sample.location()).isEqualTo(GreenSurfaceLocation.ON_GREEN);
        assertThat(sample.elevation()).isCloseTo(0.0025, org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(sample.gradient().x()).isCloseTo(0.02, org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(sample.gradient().y()).isCloseTo(-0.01, org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(sample.gradient().downhillDirection().x()).isLessThan(0.0);
        assertThat(sample.gradient().downhillDirection().y()).isGreaterThan(0.0);
    }

    @Test
    void finiteSamplesAtTheBoundaryAndCollarUseTheCanonicalGreenEdge() {
        GreenSurface surface = surface((x, y) -> x + y);

        GreenSurfaceSample edge = surface.sampleAt(new Position2d(2, 1));
        GreenSurfaceSample collar = surface.sampleAt(new Position2d(2.5, 1));
        GreenSurfaceSample outside = surface.sampleAt(new Position2d(6, 1));

        assertThat(edge.location()).isEqualTo(GreenSurfaceLocation.ON_GREEN);
        assertThat(collar.location()).isEqualTo(GreenSurfaceLocation.COLLAR);
        assertThat(outside.location()).isEqualTo(GreenSurfaceLocation.OUTSIDE);
        assertThat(collar.elevation()).isEqualTo(edge.elevation());
        assertThat(Double.isFinite(edge.elevation())).isTrue();
        assertThat(Double.isFinite(collar.gradient().magnitude())).isTrue();
        assertThat(Double.isFinite(outside.elevation())).isTrue();
    }

    @Test
    void saddleReferenceSurfaceHasTheExpectedOpposingSlopes() {
        GreenSurface surface = surface((x, y) -> 0.01 * (x - 1.0) * (y - 1.0));

        assertThat(surface.gradientAt(new Position2d(1.5, 1.0)).magnitude()).isCloseTo(0.005,
                org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(surface.gradientAt(new Position2d(1.0, 1.5)).magnitude()).isCloseTo(0.005,
                org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(surface.elevationAt(new Position2d(0.5, 0.5))).isCloseTo(0.0025,
                org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(surface.elevationAt(new Position2d(1.5, 0.5))).isCloseTo(-0.0025,
                org.assertj.core.data.Offset.offset(1.0e-12));
    }

    private static GreenSurface surface(Height height) {
        double[] elevations = new double[9];
        for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) elevations[y * 3 + x] = height.at(x, y);
        return new GreenSurface(new GreenSurfaceProvenance(GreenSurfaceVersion.V1, 1L, 2L, 3L,
                new Position2d(0, 1)), SQUARE, 0, 2, 0, 2, 3, 3, elevations);
    }

    private interface Height {
        double at(double x, double y);
    }
}
