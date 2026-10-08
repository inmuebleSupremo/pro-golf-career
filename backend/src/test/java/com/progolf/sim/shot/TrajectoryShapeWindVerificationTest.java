package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Handedness;
import com.progolf.sim.core.Rng;
import com.progolf.sim.core.RngFactory;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.TerrainRegion;
import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import java.util.List;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTimeout;

/** Deterministic compatibility and channel-separation checks for trajectory shape and directional wind. */
class TrajectoryShapeWindVerificationTest {

    private static final int CORPUS_SIZE = 2_048;
    private static final SeedCoordinate GOLDEN_COORD = new SeedCoordinate(0xC0FFEEL, 1, 1, 1, 42, 1, 7);
    private static final Position2d TEE = new Position2d(0, 0);
    private static final Position2d CUP = new Position2d(0, 200);
    private static final List<Position2d> OPEN_SQUARE = List.of(
            new Position2d(-300, -10), new Position2d(300, -10), new Position2d(300, 500), new Position2d(-300, 500));
    private static final CourseGeometry OPEN_GEOMETRY = new CourseGeometry(TEE, CUP, OPEN_SQUARE,
            List.of(new TerrainRegion(Surface.FAIRWAY, OPEN_SQUARE)));
    private static final ShotZoneProfile OPEN_ZONES = new ShotZoneProfile(List.of(
            new ZoneBand(0, 600, List.of(new LateralRegion(600, Surface.FAIRWAY)))));

    @Test
    void fixedSeedCalmStraightGoldenPreservesCarryLateralContactSettlementScoreAndRandomConsumption() {
        ShotContext context = canonical(Environment.calm(), ShotShape.STRAIGHT, Handedness.RIGHT, GOLDEN_COORD,
                new Position2d(0, 180), Attributes.uniform(65), GolferState.fresh());

        CountingRng summaryRng = new CountingRng(RngFactory.forCoordinate(GOLDEN_COORD));
        ShotOutcome summary = ShotResolver.resolveWith(context, summaryRng, false);
        assertThat(summary.trace()).isNull();
        assertThat(summary.carry()).isEqualTo(199.00778023822065);
        assertThat(summary.lateral()).isEqualTo(-1.7312904200098234);
        assertThat(summary.settlement().contact().position()).isEqualTo(new Position2d(-1.7312904200098234, 199.00778023822065));
        assertThat(summary.settlement().ball()).isEqualTo(new BallState(
                new Position2d(-1.7312904200098234, 199.00778023822065), Surface.FAIRWAY));
        assertThat(summary.strokes()).isEqualTo(1);
        assertThat(summaryRng.draws()).isEqualTo(new Draws(1, 2));

        CountingRng traceRng = new CountingRng(RngFactory.forCoordinate(GOLDEN_COORD));
        ShotOutcome traced = ShotResolver.resolveWith(context, traceRng, true);
        assertThat(traced.trace()).isNotNull();
        assertThat(traced.carry()).isEqualTo(summary.carry());
        assertThat(traced.lateral()).isEqualTo(summary.lateral());
        assertThat(traced.settlement()).isEqualTo(summary.settlement());
        assertThat(traced.strokes()).isEqualTo(summary.strokes());
        assertThat(traceRng.draws()).isEqualTo(summaryRng.draws());
    }

    @Test
    void calmStraightCorpusRemainsAtTheApprovedCarryAndDispersionBaseline() {
        Distribution distribution = distribution(Environment.calm(), ShotShape.STRAIGHT, Attributes.uniform(65),
                GolferState.fresh(), new Position2d(0, 180));

        assertThat(distribution.meanCarry()).isCloseTo(180.22161958287748, within(1e-9));
        assertThat(distribution.distanceRms()).isCloseTo(18.360356800538916, within(1e-9));
        assertThat(distribution.meanLateral()).isCloseTo(0.173230439990446, within(1e-9));
        assertThat(distribution.lateralRms()).isCloseTo(14.145347886216852, within(1e-9));
    }

    @Test
    void directionalWindSeparatesSignedDriftFromSymmetricUncertaintyAndAimAxisDecomposition() {
        Distribution calm = distribution(Environment.calm(), ShotShape.STRAIGHT, Attributes.uniform(65),
                GolferState.fresh(), new Position2d(0, 180));
        Distribution rightCross = distribution(new Environment(new WindVector(15, 0), 1.0), ShotShape.STRAIGHT,
                Attributes.uniform(65), GolferState.fresh(), new Position2d(0, 180));
        Distribution leftCross = distribution(new Environment(new WindVector(-15, 0), 1.0), ShotShape.STRAIGHT,
                Attributes.uniform(65), GolferState.fresh(), new Position2d(0, 180));
        Distribution headwind = distribution(new Environment(new WindVector(0, -15), 1.0), ShotShape.STRAIGHT,
                Attributes.uniform(65), GolferState.fresh(), new Position2d(0, 180));
        Distribution tailwind = distribution(new Environment(new WindVector(0, 15), 1.0), ShotShape.STRAIGHT,
                Attributes.uniform(65), GolferState.fresh(), new Position2d(0, 180));

        assertThat(rightCross.meanLateral()).isGreaterThan(calm.meanLateral());
        assertThat(leftCross.meanLateral()).isLessThan(calm.meanLateral());
        assertThat(rightCross.lateralStandardDeviation()).isCloseTo(leftCross.lateralStandardDeviation(), within(0.1));
        assertThat(headwind.meanCarry()).isLessThan(calm.meanCarry());
        assertThat(tailwind.meanCarry()).isGreaterThan(calm.meanCarry());

        Distribution eastAimWithEastWind = distribution(new Environment(new WindVector(15, 0), 1.0), ShotShape.STRAIGHT,
                Attributes.uniform(65), GolferState.fresh(), new Position2d(180, 0));
        Distribution eastAimCalm = distribution(Environment.calm(), ShotShape.STRAIGHT, Attributes.uniform(65),
                GolferState.fresh(), new Position2d(180, 0));
        assertThat(eastAimWithEastWind.meanLateral()).isEqualTo(eastAimCalm.meanLateral());
        assertThat(eastAimWithEastWind.meanCarry()).isGreaterThan(eastAimCalm.meanCarry());

        ScriptedRng zeroError = new ScriptedRng(0.5, 0.0, 0.0);
        ShotContext northAim = canonical(new Environment(new WindVector(15, 0), 1.0), ShotShape.STRAIGHT,
                Handedness.RIGHT, GOLDEN_COORD, new Position2d(0, 180), Attributes.uniform(65), GolferState.fresh());
        ShotOutcome deterministic = ShotResolver.resolveWith(northAim, zeroError, false);
        double windResistance = SimConstants.WIND_RESIST_FLOOR + SimConstants.WIND_RESIST_SPAN * 0.65;
        double signedDrift = 15.0 * (1.0 - windResistance) * SimConstants.CROSSWIND_DRIFT_WEIGHT;
        assertThat(deterministic.lateral()).isEqualTo(signedDrift);

        ShotOutcome positiveUncertainty = ShotResolver.resolveWith(northAim, new ScriptedRng(0.5, 1.0, 0.0), false);
        ShotOutcome negativeUncertainty = ShotResolver.resolveWith(canonical(new Environment(new WindVector(-15, 0), 1.0),
                ShotShape.STRAIGHT, Handedness.RIGHT, GOLDEN_COORD, new Position2d(0, 180), Attributes.uniform(65),
                GolferState.fresh()), new ScriptedRng(0.5, 1.0, 0.0), false);
        assertThat(positiveUncertainty.lateral() - signedDrift).isEqualTo(negativeUncertainty.lateral() + signedDrift);
    }

    @Test
    void windResistanceAndPuttingRemainIndependentOfShapeAndWindChannels() {
        ScriptedRng noError = new ScriptedRng(0.5, 0.0, 0.0);
        Environment crosswind = new Environment(new WindVector(15, 0), 1.0);
        ShotOutcome plain = ShotResolver.resolveWith(canonical(crosswind, ShotShape.STRAIGHT, Handedness.RIGHT,
                GOLDEN_COORD, new Position2d(0, 180), Attributes.uniform(65), GolferState.fresh()), noError, false);
        GolferState workable = new GolferState(0, 0, 0, 0, 0, 0, 0.5, 0);
        ShotOutcome controlled = ShotResolver.resolveWith(canonical(crosswind, ShotShape.STRAIGHT, Handedness.RIGHT,
                GOLDEN_COORD, new Position2d(0, 180), Attributes.uniform(65), workable),
                new ScriptedRng(0.5, 0.0, 0.0), false);
        assertThat(Math.abs(controlled.lateral())).isLessThan(Math.abs(plain.lateral()));

        ShotContext calmPutt = putt(Environment.calm());
        ShotContext windyPutt = putt(new Environment(new WindVector(-35, 24), 0.35));
        assertThat(ShotResolver.resolveShot(windyPutt)).isEqualTo(ShotResolver.resolveShot(calmPutt));
    }

    @Test
    void shapeExecutionUsesOnlyBoundedExistingControlInputsAndNoFixedCarryPenalty() {
        Distribution lowStraight = distribution(Environment.calm(), ShotShape.STRAIGHT, Attributes.uniform(40),
                GolferState.fresh(), new Position2d(0, 180));
        Distribution lowDraw = distribution(Environment.calm(), ShotShape.DRAW, Attributes.uniform(40),
                GolferState.fresh(), new Position2d(0, 180));
        Distribution skilledStraight = distribution(Environment.calm(), ShotShape.STRAIGHT, Attributes.uniform(90),
                new GolferState(0, 0, 0, 0, 0, 0, 0.5, 0), new Position2d(0, 180));
        Distribution skilledDraw = distribution(Environment.calm(), ShotShape.DRAW, Attributes.uniform(90),
                new GolferState(0, 0, 0, 0, 0, 0, 0.5, 0), new Position2d(0, 180));

        assertThat(lowDraw.meanCarry()).isEqualTo(lowStraight.meanCarry());
        assertThat(lowDraw.lateralStandardDeviation()).isGreaterThan(lowStraight.lateralStandardDeviation());
        assertThat(skilledDraw.lateralStandardDeviation() / skilledStraight.lateralStandardDeviation())
                .isLessThan(lowDraw.lateralStandardDeviation() / lowStraight.lateralStandardDeviation());
    }

    @Test
    void highVolumeBackgroundResolutionStaysTraceFreeAndDeterministic() {
        assertTimeout(Duration.ofSeconds(2), () -> {
            double firstPass = summarySignature();
            double secondPass = summarySignature();
            assertThat(secondPass).isEqualTo(firstPass);
        });
    }

    private static ShotContext canonical(Environment environment, ShotShape shape, Handedness handedness,
                                         SeedCoordinate coordinate, Position2d aim, Attributes attributes, GolferState state) {
        double requestedCarry = TEE.distanceTo(aim);
        ShotDecision decision = new ShotDecision(Club.IRON, requestedCarry, 0.0, Strategy.BALANCED,
                ClubSpec.of(ClubId.SIX_IRON), ShotFamily.FULL, shape);
        return new ShotContext(attributes, state, environment, requestedCarry, OPEN_ZONES, decision, coordinate,
                Surface.FAIRWAY, 0.0, new BallState(TEE, Surface.FAIRWAY), OPEN_GEOMETRY, CUP, aim, handedness);
    }

    private static ShotContext putt(Environment environment) {
        return new ShotContext(Attributes.uniform(65), GolferState.fresh(), environment, 12, OPEN_ZONES,
                new ShotDecision(Club.PUTTER, 12, 0, Strategy.BALANCED, ClubSpec.of(ClubId.PUTTER)), GOLDEN_COORD,
                Surface.GREEN, 0, new BallState(new Position2d(0, 188), Surface.GREEN), OPEN_GEOMETRY, CUP, CUP,
                Handedness.RIGHT);
    }

    private static Distribution distribution(Environment environment, ShotShape shape, Attributes attributes,
                                             GolferState state, Position2d aim) {
        double carry = 0;
        double carryErrorSquared = 0;
        double lateral = 0;
        double lateralSquared = 0;
        for (int sample = 1; sample <= CORPUS_SIZE; sample++) {
            ShotOutcome outcome = ShotResolver.resolveShot(canonical(environment, shape, Handedness.RIGHT,
                    new SeedCoordinate(0xC0FFEEL, 1, 1, 1, 42, 1, sample), aim, attributes, state));
            carry += outcome.carry();
            double carryError = outcome.carry() - TEE.distanceTo(aim);
            carryErrorSquared += carryError * carryError;
            lateral += outcome.lateral();
            lateralSquared += outcome.lateral() * outcome.lateral();
        }
        double meanCarry = carry / CORPUS_SIZE;
        double meanLateral = lateral / CORPUS_SIZE;
        return new Distribution(meanCarry, Math.sqrt(carryErrorSquared / CORPUS_SIZE), meanLateral,
                Math.sqrt(lateralSquared / CORPUS_SIZE), Math.sqrt(lateralSquared / CORPUS_SIZE - meanLateral * meanLateral));
    }

    private static double summarySignature() {
        double signature = 0;
        for (int sample = 1; sample <= CORPUS_SIZE; sample++) {
            ShotOutcome outcome = ShotResolver.resolveShot(canonical(Environment.calm(), ShotShape.STRAIGHT,
                    Handedness.RIGHT, new SeedCoordinate(0xC0FFEEL, 1, 1, 2, 42, 1, sample),
                    new Position2d(0, 180), Attributes.uniform(65), GolferState.fresh()));
            assertThat(outcome.trace()).isNull();
            signature += outcome.carry() * 31.0 + outcome.lateral();
        }
        return signature;
    }

    private record Distribution(double meanCarry, double distanceRms, double meanLateral, double lateralRms,
                                double lateralStandardDeviation) { }
    private record Draws(int doubles, int gaussians) { }

    private static final class CountingRng implements Rng {
        private final Rng delegate;
        private int doubles;
        private int gaussians;

        private CountingRng(Rng delegate) { this.delegate = delegate; }
        @Override public long nextLong() { return delegate.nextLong(); }
        @Override public double nextDouble() { doubles++; return delegate.nextDouble(); }
        @Override public double nextGaussian() { gaussians++; return delegate.nextGaussian(); }
        private Draws draws() { return new Draws(doubles, gaussians); }
    }

    private static final class ScriptedRng implements Rng {
        private final double uniform;
        private final double[] gaussians;
        private int index;

        private ScriptedRng(double uniform, double... gaussians) {
            this.uniform = uniform;
            this.gaussians = gaussians;
        }
        @Override public long nextLong() { return 0; }
        @Override public double nextDouble() { return uniform; }
        @Override public double nextGaussian() { return gaussians[index++]; }
    }
}
