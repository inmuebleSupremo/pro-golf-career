package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.support.Fixtures;
import org.junit.jupiter.api.Test;

/** Shot-resolution spec: controlled randomness, skill expression over samples, monotonicity, strategy. */
class ShotStatisticsTest {

    private static final int N = 20_000;

    private static double meanRemaining(Attributes attrs, Strategy strategy) {
        double sum = 0;
        for (int i = 1; i <= N; i++) {
            sum += ShotResolver.resolveShot(Fixtures.driverShot(attrs, strategy, i)).distanceRemaining();
        }
        return sum / N;
    }

    @Test
    void averageOutcomesDominateAndExtremesAreRare() {
        Attributes attrs = Attributes.uniform(60);
        int nearTarget = 0;
        int catastrophic = 0;
        double best = Double.MAX_VALUE;
        for (int i = 1; i <= N; i++) {
            double remaining = ShotResolver.resolveShot(Fixtures.driverShot(attrs, Strategy.BALANCED, i)).distanceRemaining();
            // A full driver to a 250y target scatters ~20-30y wide at realistic tour dispersion; "near" is a
            // fairway-width band, not tap-in range (that band was tied to the old, too-tight calibration).
            if (remaining <= 30) {
                nearTarget++;
            }
            if (remaining > 60) {
                catastrophic++;
            }
            best = Math.min(best, remaining);
        }
        // Central tendency: most shots cluster near the target. The band is a fairway-width scatter at the
        // current realistic tour dispersion — a below-average golfer misses it a fair share of the time, which
        // is the point (pros hit only ~60-65% of fairways); this guards that the bulk still finds it.
        assertThat((double) nearTarget / N).isGreaterThan(0.60);
        // Extremes: rare.
        assertThat((double) catastrophic / N).isLessThan(0.05);
        // Exceptional shots still happen.
        assertThat(best).isLessThan(3.0);
    }

    @Test
    void higherRelevantAttributeImprovesMeanOutcome() {
        Attributes weak = Attributes.uniform(50).with(Attribute.DRIVING_ACCURACY, 30);
        Attributes strong = Attributes.uniform(50).with(Attribute.DRIVING_ACCURACY, 90);
        assertThat(meanRemaining(strong, Strategy.BALANCED))
                .isLessThan(meanRemaining(weak, Strategy.BALANCED));
    }

    @Test
    void lowAttributeGolferStillProducesOccasionalExceptionalShot() {
        Attributes weak = Attributes.uniform(25);
        double best = Double.MAX_VALUE;
        for (int i = 1; i <= N; i++) {
            best = Math.min(best, ShotResolver.resolveShot(Fixtures.driverShot(weak, Strategy.BALANCED, i)).distanceRemaining());
        }
        assertThat(best).isLessThan(4.0);
    }

    @Test
    void unrelatedAttributeDoesNotWorsenExpectedOutcome() {
        // Putting attributes are irrelevant to a driver shot -> outcomes (and mean) must be unchanged.
        Attributes low = Attributes.uniform(50).with(Attribute.PUTTING_PROXIMITY, 10);
        Attributes high = Attributes.uniform(50).with(Attribute.PUTTING_PROXIMITY, 95);
        assertThat(meanRemaining(high, Strategy.BALANCED))
                .isEqualTo(meanRemaining(low, Strategy.BALANCED));
    }

    @Test
    void aggressiveStrategyWidensDispersion() {
        double conservativeVar = lateralVariance(Strategy.CONSERVATIVE);
        double aggressiveVar = lateralVariance(Strategy.AGGRESSIVE);
        assertThat(aggressiveVar).isGreaterThan(conservativeVar);
    }

    private static double lateralVariance(Strategy strategy) {
        Attributes attrs = Attributes.uniform(60);
        double sum = 0;
        double sumSq = 0;
        for (int i = 1; i <= N; i++) {
            double lateral = ShotResolver.resolveShot(Fixtures.driverShot(attrs, strategy, i)).lateral();
            sum += lateral;
            sumSq += lateral * lateral;
        }
        double mean = sum / N;
        return sumSq / N - mean * mean;
    }
}
