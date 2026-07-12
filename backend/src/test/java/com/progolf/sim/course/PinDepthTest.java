package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import org.junit.jupiter.api.Test;

/** hole-spatial-model: the pin's depth shifts the green off-centre — a back pin leaves the over-green
 *  trouble closer behind, a front pin shortens the safe run-up in front. */
class PinDepthTest {

    private static final double R = 150.0;

    private static GeneratedHole hole() {
        return CourseGenerator.generate(new SeedCoordinate(0xC0FFEEL, 1, 1, 1, 1, 1, 0),
                EnvironmentClassification.PARKLAND).holes().get(3);
    }

    /** The distance behind the pin at which the green ends (surface at centre stops being GREEN). */
    private static double greenEndsBehind(ShotZoneProfile p) {
        for (double d = 0.0; d < 60.0; d += 0.25) {
            if (p.surfaceAt(R + d, 0.0) != Surface.GREEN) {
                return d;
            }
        }
        return 60.0;
    }

    /** The distance in front of the pin at which the green starts (surface at centre becomes GREEN). */
    private static double greenStartsInFront(ShotZoneProfile p) {
        for (double d = 0.0; d < 60.0; d += 0.25) {
            if (p.surfaceAt(R - d, 0.0) != Surface.GREEN) {
                return d;
            }
        }
        return 60.0;
    }

    @Test
    void aBackPinLeavesTheOverGreenTroubleCloserBehind() {
        GeneratedHole hole = hole();
        double back = greenEndsBehind(HoleZones.profileFor(hole, R, +6.0));
        double front = greenEndsBehind(HoleZones.profileFor(hole, R, -6.0));
        assertThat(back).isLessThan(front); // less green behind a back pin -> trouble closer when long
    }

    @Test
    void aFrontPinShortensTheSafeRunUp() {
        GeneratedHole hole = hole();
        double front = greenStartsInFront(HoleZones.profileFor(hole, R, -6.0));
        double back = greenStartsInFront(HoleZones.profileFor(hole, R, +6.0));
        assertThat(front).isLessThan(back); // less green in front of a front pin -> short misses punished
    }

    @Test
    void aCentrePinIsSymmetricAndUnchanged() {
        GeneratedHole hole = hole();
        // A zero depth offset reproduces the original symmetric green (default overload).
        ShotZoneProfile centre = HoleZones.profileFor(hole, R, 0.0);
        ShotZoneProfile original = HoleZones.profileFor(hole, R);
        assertThat(greenEndsBehind(centre)).isEqualTo(greenEndsBehind(original));
        assertThat(greenEndsBehind(centre)).isEqualTo(greenStartsInFront(centre)); // symmetric front/back
    }
}
