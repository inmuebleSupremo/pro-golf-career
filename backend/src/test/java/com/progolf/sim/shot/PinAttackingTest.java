package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import java.util.List;
import org.junit.jupiter.api.Test;

/** shot-resolution (pin-attacking): distance is measured to the actual pin, so attacking a tucked flag
 *  leaves closer approaches (birdie looks) at the cost of hitting fewer greens. */
class PinAttackingTest {

    private static final Attributes ATTRS = Attributes.uniform(62);
    private static final double DIST = 115.0;
    private static final double PIN = 8.0; // pin tucked +8 toward a bunker

    /** A green complex centred on {@code DIST}, green half-width 12, flanked by fringe/bunker/rough. */
    private static ShotZoneProfile green() {
        return new ShotZoneProfile(List.of(
                new ZoneBand(0, DIST - 12, List.of(new LateralRegion(200, Surface.FAIRWAY))),
                new ZoneBand(DIST - 12, DIST + 12, List.of(
                        new LateralRegion(12, Surface.GREEN),
                        new LateralRegion(16, Surface.FRINGE),
                        new LateralRegion(22, Surface.BUNKER),
                        new LateralRegion(34, Surface.PRIMARY_ROUGH))),
                new ZoneBand(DIST + 12, DIST + 72, List.of(new LateralRegion(200, Surface.PRIMARY_ROUGH)))));
    }

    /** [meanRemaining, greenRate, closeRate(<3yd)] for a wedge aimed at {@code aim}, pin at +8. */
    private static double[] resolve(double aim) {
        int n = 30000, green = 0, close = 0;
        double sumRem = 0;
        for (int i = 1; i <= n; i++) {
            ShotContext ctx = new ShotContext(ATTRS, GolferState.fresh(), Environment.calm(), DIST, green(),
                    new ShotDecision(Club.WEDGE, DIST, aim, Strategy.BALANCED),
                    new SeedCoordinate(0xC0FFEEL, 1, 1, 1, 7, 1, i), Surface.FAIRWAY, PIN);
            ShotOutcome o = ShotResolver.resolveShot(ctx);
            sumRem += o.distanceRemaining();
            if (o.finalSurface() == Surface.GREEN) green++;
            if (o.distanceRemaining() < 3.0) close++;
        }
        return new double[]{sumRem / n, (double) green / n, (double) close / n};
    }

    @Test
    void attackingThePinLeavesCloserApproachesButHitsFewerGreens() {
        double[] center = resolve(0.0);  // conservative: aim green centre
        double[] pin = resolve(PIN);      // aggressive: aim at the tucked pin

        // Closer to the hole on average, and more birdie-range approaches...
        assertThat(pin[0]).as("mean remaining").isLessThan(center[0]);
        assertThat(pin[2]).as("within 3yd").isGreaterThan(center[2]);
        // ...at the cost of holding the green less often (the flanking bunker bites).
        assertThat(pin[1]).as("green rate").isLessThan(center[1]);
    }

    @Test
    void distanceIsMeasuredToTheActualPinNotGreenCentre() {
        // The same centre-aimed shots are farther from a tucked pin than from a centre pin — distance is
        // measured to the actual hole, so where the pin sits matters.
        double toCentrePin = meanRemainingCentreAim(0.0);
        double toTuckedPin = meanRemainingCentreAim(PIN);
        assertThat(toTuckedPin).isGreaterThan(toCentrePin);
    }

    /** Mean distance to a pin at {@code pinLateral} for shots aimed at the green centre. */
    private static double meanRemainingCentreAim(double pinLateral) {
        int n = 20000;
        double sum = 0;
        for (int i = 1; i <= n; i++) {
            ShotContext ctx = new ShotContext(ATTRS, GolferState.fresh(), Environment.calm(), DIST, green(),
                    new ShotDecision(Club.WEDGE, DIST, 0.0, Strategy.CONSERVATIVE),
                    new SeedCoordinate(0xC0FFEEL, 1, 1, 1, 9, 1, i), Surface.FAIRWAY, pinLateral);
            sum += ShotResolver.resolveShot(ctx).distanceRemaining();
        }
        return sum / n;
    }
}
