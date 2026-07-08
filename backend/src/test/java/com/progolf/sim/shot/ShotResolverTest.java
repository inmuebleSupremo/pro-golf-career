package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.support.Fixtures;
import java.lang.reflect.RecordComponent;
import org.junit.jupiter.api.Test;

/** Shot-resolution spec: decision guard, complete outcome, reproducibility, shared (control-free) model. */
class ShotResolverTest {

    @Test
    void incompleteDecisionIsRejectedBeforeAnyOutcome() {
        assertThatThrownBy(() -> ShotResolver.resolveShot(null))
                .isInstanceOf(NullPointerException.class);
        // A ShotDecision cannot even be constructed without a club or strategy.
        assertThatThrownBy(() -> ShotDecision.straight(null, 250, Strategy.BALANCED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ShotDecision(Club.DRIVER, 250, 0, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void everyOutcomeIsFullyPopulated() {
        ShotOutcome outcome = ShotResolver.resolveShot(Fixtures.driverShot(Attributes.uniform(60), Strategy.BALANCED, 1));
        assertThat(outcome.finalSurface()).isNotNull();
        assertThat(outcome.factors()).isNotNull();
        assertThat(outcome.strokes()).isGreaterThanOrEqualTo(1);
        assertThat(outcome.distanceRemaining()).isGreaterThanOrEqualTo(0.0);
        assertThat(Double.isFinite(outcome.carry())).isTrue();
        assertThat(Double.isFinite(outcome.lateral())).isTrue();
    }

    @Test
    void resolvingTheSameShotTwiceIsStable() {
        ShotContext ctx = Fixtures.driverShot(Attributes.uniform(55), Strategy.BALANCED, 7);
        assertThat(ShotResolver.resolveShot(ctx)).isEqualTo(ShotResolver.resolveShot(ctx));
    }

    @Test
    void reResolvingCoordinatesInAnyOrderReproducesResults() {
        Attributes attrs = Attributes.uniform(50);
        // Resolve shots 1..50 forward, capture; resolve 50..1 backward, compare per coordinate.
        ShotOutcome[] forward = new ShotOutcome[51];
        for (int i = 1; i <= 50; i++) {
            forward[i] = ShotResolver.resolveShot(Fixtures.driverShot(attrs, Strategy.BALANCED, i));
        }
        for (int i = 50; i >= 1; i--) {
            ShotOutcome again = ShotResolver.resolveShot(Fixtures.driverShot(attrs, Strategy.BALANCED, i));
            assertThat(again).isEqualTo(forward[i]);
        }
    }

    @Test
    void hazardOutcomeIncursPenaltyStrokes() {
        // Aim the target lateral far offline so the ball finishes out of bounds; penalty must apply.
        SeedCoordinate coord = Fixtures.coordinate().withShot(3);
        ShotContext ctx = new ShotContext(
                Attributes.uniform(50),
                GolferState.fresh(),
                Environment.calm(),
                250.0,
                Fixtures.standardProfile(),
                new ShotDecision(Club.DRIVER, 250.0, 200.0, Strategy.BALANCED), // wildly offline aim
                coord);
        ShotOutcome outcome = ShotResolver.resolveShot(ctx);
        assertThat(outcome.finalSurface()).isEqualTo(Surface.OUT_OF_BOUNDS);
        assertThat(outcome.hazardEntered()).isTrue();
        assertThat(outcome.penaltyStrokes()).isEqualTo(1);
        assertThat(outcome.strokes()).isEqualTo(2);
    }

    @Test
    void modelHasNoControlTypeInput() {
        // The shared model must not distinguish human from AI: no control-type field exists.
        for (RecordComponent rc : ShotContext.class.getRecordComponents()) {
            String name = rc.getName().toLowerCase();
            assertThat(name).doesNotContain("control");
            assertThat(name).doesNotContain("human");
            assertThat(name).doesNotContain("ishuman");
            assertThat(name).doesNotContain("isai");
        }
    }
}
