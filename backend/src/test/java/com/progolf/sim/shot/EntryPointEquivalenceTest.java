package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import com.progolf.sim.support.Fixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Shot-resolution spec: resolveRound composes the same per-shot core as resolveShot; determinism. */
class EntryPointEquivalenceTest {

    /** A trivial par-4-ish hole with a generous single-band profile at every remaining distance. */
    private static final HoleModel HOLE = new HoleModel() {
        @Override
        public double startDistance() {
            return 410.0;
        }

        @Override
        public ShotZoneProfile zoneProfileFor(double remainingDistance) {
            ZoneBand band = new ZoneBand(0.0, 600.0, List.of(
                    new LateralRegion(25.0, Surface.FAIRWAY),
                    new LateralRegion(45.0, Surface.PRIMARY_ROUGH),
                    new LateralRegion(70.0, Surface.DEEP_ROUGH)));
            return new ShotZoneProfile(List.of(band));
        }
    };

    private static final SeedCoordinate HOLE_COORD = new SeedCoordinate(Fixtures.MASTER_SEED, 2, 5, 3, 77, 9, 0);

    @Test
    void resolveRoundIsDeterministic() {
        RoundOutcome a = resolve();
        RoundOutcome b = resolve();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void firstShotFromResolveRoundEqualsDirectResolveShot() {
        Attributes attrs = Attributes.uniform(65);
        GolferState state = GolferState.fresh();
        Environment env = Environment.calm();

        RoundOutcome round = RoundResolver.resolveHole(HOLE, attrs, state, env, Strategy.BALANCED, HOLE_COORD);

        // Reconstruct the exact shot-1 context the round resolver used, then resolve it directly.
        StrategyPolicy policy = new StrategyPolicy(Strategy.BALANCED);
        ShotContext shot1 = RoundResolver.buildContext(HOLE, attrs, state, env, policy, HOLE.startDistance(), HOLE_COORD, 1);
        ShotOutcome direct = ShotResolver.resolveShot(shot1);

        assertThat(round.shots().get(0)).isEqualTo(direct);
    }

    @Test
    void roundHolesOutAndAccumulatesStrokes() {
        RoundOutcome round = resolve();
        assertThat(round.shots()).isNotEmpty();
        assertThat(round.shots().size()).isLessThan(SimConstants.MAX_SHOTS_PER_HOLE);
        int summed = round.shots().stream().mapToInt(ShotOutcome::strokes).sum();
        assertThat(round.totalStrokes()).isEqualTo(summed);

        // The round must actually hole out (not just hit the shot cap): the final shot is not a hazard
        // and finishes within the holed threshold. This guards against distance-independent dispersion.
        ShotOutcome last = round.shots().get(round.shots().size() - 1);
        assertThat(last.hazardEntered()).isFalse();
        assertThat(last.distanceRemaining()).isLessThanOrEqualTo(SimConstants.HOLED_THRESHOLD);
    }

    @Test
    void competentGolferHolesOutInRealisticStrokeCount() {
        RoundOutcome round = RoundResolver.resolveHole(HOLE, Attributes.uniform(70), GolferState.fresh(),
                Environment.calm(), Strategy.BALANCED, HOLE_COORD);
        ShotOutcome last = round.shots().get(round.shots().size() - 1);
        assertThat(last.distanceRemaining()).isLessThanOrEqualTo(SimConstants.HOLED_THRESHOLD);
        // A competent golfer on a ~410y hole should hole out in a believable number of strokes.
        assertThat(round.totalStrokes()).isBetween(2, 8);
    }

    private static RoundOutcome resolve() {
        return RoundResolver.resolveHole(HOLE, Attributes.uniform(65), GolferState.fresh(),
                Environment.calm(), Strategy.BALANCED, HOLE_COORD);
    }
}
