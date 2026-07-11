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

/** shot-resolution: each club carries its own lateral/distance dispersion profile (per-club calibration). */
class PerClubDispersionTest {

    private static final int N = 20000;
    private static final Attributes ATTRS = Attributes.uniform(65);

    private static ShotZoneProfile openProfile() {
        return new ShotZoneProfile(List.of(new ZoneBand(0.0, 600.0,
                List.of(new LateralRegion(300.0, Surface.FAIRWAY)))));
    }

    /** [lateralSd, distanceSd] in yards for a straight shot with {@code club} aimed at {@code dist}. */
    private static double[] spread(Club club, double dist) {
        double sl = 0, sl2 = 0, sd = 0, sd2 = 0;
        for (int i = 1; i <= N; i++) {
            ShotContext ctx = new ShotContext(ATTRS, GolferState.fresh(), Environment.calm(), dist,
                    openProfile(), ShotDecision.straight(club, dist, Strategy.BALANCED),
                    new SeedCoordinate(0xC0FFEEL, 1, 1, 1, 42, 1, i), Surface.FAIRWAY);
            ShotOutcome o = ShotResolver.resolveShot(ctx);
            sl += o.lateral(); sl2 += o.lateral() * o.lateral();
            double e = o.carry() - dist; sd += e; sd2 += e * e;
        }
        return new double[]{Math.sqrt(sl2 / N - (sl / N) * (sl / N)), Math.sqrt(sd2 / N - (sd / N) * (sd / N))};
    }

    @Test
    void aWedgeIsMorePreciseThanAnIronAtTheSameDistance() {
        // Same distance isolates the per-club multipliers: the wedge is the precision club (tighter both ways).
        double[] iron = spread(Club.IRON, 110);
        double[] wedge = spread(Club.WEDGE, 110);
        assertThat(wedge[0]).as("wedge lateral").isLessThan(iron[0]);
        assertThat(wedge[1]).as("wedge distance").isLessThan(iron[1]);
    }

    @Test
    void theDriverSpraysWiderOffTheTeeThanTheWedge() {
        // The driver has the widest offline spread of any club; the wedge the tightest.
        assertThat(spread(Club.DRIVER, 285)[0]).isGreaterThan(spread(Club.WEDGE, 110)[0]);
    }

    @Test
    void everyClubHasPositiveDispersionMultipliers() {
        for (Club c : Club.values()) {
            assertThat(c.lateralDispersion()).isPositive();
            assertThat(c.distanceDispersion()).isPositive();
        }
    }
}
