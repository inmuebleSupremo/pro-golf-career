package com.progolf.sim.play;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.PuttIntent;
import com.progolf.sim.shot.AimEnvelope;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.ShotContext;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotFrame;
import com.progolf.sim.shot.ShotFamilyEligibility;
import com.progolf.sim.shot.ShotAim;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.ShotResolver;
import com.progolf.sim.shot.SimConstants;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.shot.StrategyPolicy;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.course.Position2d;
import java.util.Objects;

/**
 * An interactive single hole a human plays shot-by-shot through the shot engine (spec: playable-event) —
 * used to play a sudden-death playoff hole. Its per-shot loop mirrors {@code RoundResolver.resolveHole}
 * exactly: the same penalty-hazard recovery (water-drop vs out-of-bounds stroke-and-distance), holed
 * threshold, and shot cap, seeded at the supplied hole coordinate via {@code coordinate.withShot(n)}. So a
 * fully-simmed hole is identical to the automatic
 * playoff-hole resolution for the same inputs and seed, and playing by hand differs only by the human's
 * decisions.
 *
 * <p>Unlike {@link PlayableRound} (which addresses holes with {@code base.withHole(h)}), a playoff hole is
 * seeded with the hole coordinate already fixed — the automatic sudden-death path seeds each playoff hole
 * at {@code (…, playoffRound, fieldIndex, holeNumber, shot)} — so this takes the hole coordinate directly.
 */
public final class PlayableHole {

    private final int holeNumber;
    private final int par;
    private final Attributes attributes;
    private final GolferState state;
    private final HoleModel model;
    private final Environment environment;
    private final SeedCoordinate coordinate;
    private final StrategyPolicy simPolicy;

    private int shotNumber = 1;
    private double remaining;
    private int strokes;
    private Surface lie = Surface.TEE_BOX;
    private BallState ball;
    private boolean complete;

    public PlayableHole(int holeNumber, int par, Attributes attributes, GolferState state, HoleModel model,
                        Environment environment, SeedCoordinate coordinate, Strategy simStrategy) {
        this.holeNumber = holeNumber;
        this.par = par;
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.state = Objects.requireNonNull(state, "state");
        this.model = Objects.requireNonNull(model, "model");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.coordinate = Objects.requireNonNull(coordinate, "coordinate");
        this.simPolicy = new StrategyPolicy(Objects.requireNonNull(simStrategy, "simStrategy"));
        this.remaining = model.startDistance();
        this.ball = model.geometry() == null ? null : new BallState(model.geometry().tee(), Surface.TEE_BOX);
    }

    /** Whether the hole has been holed out (or hit the shot cap). */
    public boolean isComplete() {
        return complete;
    }

    /** The situation for the current shot. */
    public ShotSituation situation() {
        requireNotComplete();
        return new ShotSituation(holeNumber, par, shotNumber, strokes, remaining, lie,
                model.pinLateral(), model.zoneProfileFor(remaining));
    }

    /** Plays the current shot with the human's decision (club / target / risk). */
    public ShotOutcome playShot(ShotDecision decision) {
        requireNotComplete();
        return resolveOne(Objects.requireNonNull(decision, "decision"), null, true);
    }

    /** Human spatial intent path for sudden-death play. */
    public ShotOutcome playShot(BallStrikeIntent intent) {
        requireNotComplete();
        if (ball == null || model.geometry() == null) throw new IllegalStateException("spatial intent requires canonical geometry");
        Position2d target = new Position2d(intent.aimPoint().x(), intent.aimPoint().y());
        validateAim(target);
        ShotFamilyEligibility.Result eligibility = ShotFamilyEligibility.evaluate(lie,
                com.progolf.sim.shot.ClubSpec.of(intent.club()), intent.shotFamily());
        if (!eligibility.allowed()) throw new IllegalArgumentException(eligibility.reason());
        double requestedCarry = ball.position().distanceTo(target);
        return resolveOne(ShotDecision.fromIntent(intent, requestedCarry, Strategy.BALANCED), target, true);
    }

    /** Deliberate entry point for the existing non-spatial putting model. */
    public ShotOutcome playPutt(PuttIntent intent) {
        requireNotComplete();
        Objects.requireNonNull(intent, "intent");
        if (lie != Surface.GREEN && lie != Surface.FRINGE) throw new IllegalArgumentException("putting is available only on green or fringe");
        return resolveOne(new ShotDecision(com.progolf.sim.shot.Club.PUTTER, remaining, 0.0, Strategy.BALANCED),
                ball == null ? null : model.cupPosition(), true);
    }

    /** Sims the current shot with the automatic policy. */
    public ShotOutcome simShot() {
        requireNotComplete();
        SimShot shot = simShotPlan();
        return resolveOne(shot.decision(), shot.aimTarget(), true);
    }

    /** Sims the rest of the hole with the automatic policy. */
    public void simHole() {
        while (!complete) {
            SimShot shot = simShotPlan();
            resolveOne(shot.decision(), shot.aimTarget(), false);
        }
    }

    private ShotOutcome resolveOne(ShotDecision decision, Position2d humanAimTarget, boolean materializeTrace) {
        double preShotRemaining = remaining;
        ShotContext context = ball == null
                ? new ShotContext(attributes, state, environment, remaining, model.zoneProfileFor(remaining), decision,
                coordinate.withShot(shotNumber), lie, model.pinLateral())
                : spatialContext(decision, humanAimTarget);
        ShotOutcome outcome = materializeTrace ? ShotResolver.resolveShotWithTrace(context) : ShotResolver.resolveShot(context);

        strokes += outcome.strokes();
        lie = outcome.finalSurface();
        boolean holed;
        if (outcome.settlement() != null) {
            ball = outcome.settlement().ball();
            lie = ball.lie();
            remaining = ball.position().distanceTo(model.cupPosition());
            holed = !outcome.hazardEntered() && remaining <= SimConstants.HOLED_THRESHOLD;
        } else if (outcome.hazardEntered()) {
            // Penalty-hazard recovery, identical to RoundResolver so simmed == auto (spec: shot-resolution).
            if (outcome.finalSurface() == Surface.WATER) {
                remaining = Math.min(preShotRemaining, outcome.distanceRemaining() + SimConstants.WATER_DROP_SETBACK);
                lie = Surface.PRIMARY_ROUGH; // dropped in rough near the hazard
            } else {
                remaining = preShotRemaining; // out of bounds: stroke-and-distance
            }
            holed = false;
        } else {
            remaining = outcome.distanceRemaining();
            holed = remaining <= SimConstants.HOLED_THRESHOLD;
        }
        shotNumber++;
        if (holed || shotNumber > SimConstants.MAX_SHOTS_PER_HOLE) {
            complete = true;
        }
        return outcome;
    }

    /** Total strokes taken on the hole (final once complete). */
    public int strokes() {
        return strokes;
    }

    /** Current canonical next-shot origin, present for generated playoff holes. */
    public BallState ballState() {
        return ball;
    }

    /** The exact setup-specific model used to resolve this playoff hole. */
    public HoleModel model() {
        return model;
    }

    private SimShot simShotPlan() {
        if (ball == null) return new SimShot(simPolicy.decide(remaining, lie, model.pinLateral(), attributes, par), null);
        com.progolf.sim.shot.ShotIntent selected = simPolicy.decideShotIntent(model, ball, remaining, lie, attributes, par,
                environment);
        if (selected instanceof PuttIntent) {
            return new SimShot(new ShotDecision(com.progolf.sim.shot.Club.PUTTER, remaining, 0.0, Strategy.BALANCED), model.cupPosition());
        }
        BallStrikeIntent intent = (BallStrikeIntent) selected;
        Position2d aim = new Position2d(intent.aimPoint().x(), intent.aimPoint().y());
        return new SimShot(ShotDecision.fromIntent(intent, ball.position().distanceTo(aim),
                simPolicy.executionStrategyFor(lie)), aim);
    }


    private ShotContext spatialContext(ShotDecision decision, Position2d humanAimTarget) {
        ShotAim.Reference aim = humanAimTarget == null ? ShotAim.forBall(model, ball, decision.strategy())
                : new ShotAim.Reference(humanAimTarget, 0.0, false);
        return new ShotContext(attributes, state, environment, remaining, model.zoneProfileFor(remaining), decision,
                coordinate.withShot(shotNumber), lie, aim.pinLateral(), ball, model.geometry(), model.cupPosition(), aim.target());
    }

    private void requireNotComplete() {
        if (complete) {
            throw new IllegalStateException("the hole is complete");
        }
    }

    private void validateAim(Position2d target) {
        var boundary = model.geometry().playableBoundary();
        AimEnvelope envelope = new AimEnvelope(
                boundary.stream().mapToDouble(Position2d::x).min().orElseThrow() - 100.0,
                boundary.stream().mapToDouble(Position2d::x).max().orElseThrow() + 100.0,
                boundary.stream().mapToDouble(Position2d::y).min().orElseThrow() - 100.0,
                boundary.stream().mapToDouble(Position2d::y).max().orElseThrow() + 100.0);
        if (!envelope.contains(new AimPoint(target.x(), target.y()))) {
            throw new IllegalArgumentException("aim point is outside the planning envelope");
        }
    }

    private record SimShot(ShotDecision decision, Position2d aimTarget) { }
}
