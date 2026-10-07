package com.progolf.sim.shot;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves a full hole for a golfer without an interactive loop (the {@code resolveRound} entry point,
 * spec: shot-resolution). It does NOT re-implement any shot mathematics: it loops the identical
 * {@link ShotResolver} per-shot core, deriving each decision from a {@link StrategyPolicy} and seeding
 * every shot at the same coordinate a {@code resolveShot} call would use. This makes distribution
 * equivalence across entry points true by construction (design D6).
 */
public final class RoundResolver {

    private RoundResolver() {
    }

    /** Resolves one hole for a golfer, returning every shot and the total strokes. */
    public static RoundOutcome resolveHole(
            HoleModel hole,
            Attributes attributes,
            GolferState state,
            Environment environment,
            Strategy strategy,
            SeedCoordinate holeCoordinate) {

        Objects.requireNonNull(hole, "hole");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(holeCoordinate, "holeCoordinate");

        StrategyPolicy policy = new StrategyPolicy(strategy);
        List<ShotOutcome> shots = new ArrayList<>();
        int totalStrokes = 0;
        double remaining = hole.startDistance();
        Surface lie = Surface.TEE_BOX; // the surface the next shot is played from (a green lie => a putt)
        BallState ball = hole.geometry() == null ? null : new BallState(hole.geometry().tee(), Surface.TEE_BOX);

        for (int shotNo = 1; shotNo <= SimConstants.MAX_SHOTS_PER_HOLE; shotNo++) {
            double preShotRemaining = remaining;
            ShotContext context = ball == null
                    ? buildContext(hole, attributes, state, environment, policy, remaining, lie, holeCoordinate, shotNo)
                    : spatialContext(hole, attributes, state, environment, policy, remaining, lie, holeCoordinate, shotNo, ball);
            ShotOutcome outcome = ShotResolver.resolveShot(context);
            shots.add(outcome);
            totalStrokes += outcome.strokes();
            lie = outcome.finalSurface(); // updated unconditionally, matching the interactive PlayableRound
            if (outcome.settlement() != null) {
                ball = outcome.settlement().ball();
                lie = ball.lie();
                remaining = ball.position().distanceTo(hole.cupPosition());
                if (!outcome.hazardEntered() && remaining <= SimConstants.HOLED_THRESHOLD) {
                    break;
                }
            } else if (outcome.hazardEntered()) {
                // Penalty-hazard recovery (spec: shot-resolution). The penalty stroke is already counted in
                // outcome.strokes(); the loop only decides where the next shot is played from.
                if (outcome.finalSurface() == Surface.WATER) {
                    // Water-drop: drop near where the ball entered the hazard and play forward, from a rough
                    // lie — clamped so it never leaves the player worse off than stroke-and-distance.
                    remaining = Math.min(preShotRemaining, outcome.distanceRemaining() + SimConstants.WATER_DROP_SETBACK);
                    lie = Surface.PRIMARY_ROUGH;
                } else {
                    // Out of Bounds: stroke-and-distance — play resumes from the previous position.
                    remaining = preShotRemaining;
                }
            } else {
                remaining = outcome.distanceRemaining();
                if (remaining <= SimConstants.HOLED_THRESHOLD) {
                    break;
                }
            }
        }

        return new RoundOutcome(shots, totalStrokes);
    }

    private static ShotContext spatialContext(HoleModel hole, Attributes attributes, GolferState state,
                                              Environment environment, StrategyPolicy policy, double remainingDistance,
                                              Surface lie, SeedCoordinate holeCoordinate, int shotNo, BallState ball) {
        // The AI emits the shared intent. The compatibility shape is derived solely from that intent.
        ShotIntent intent = policy.decideShotIntent(hole, ball, remainingDistance, lie, attributes, hole.par());
        if (intent instanceof PuttIntent) {
            return new ShotContext(attributes, state, environment, remainingDistance, hole.zoneProfileFor(remainingDistance),
                    new ShotDecision(Club.PUTTER, remainingDistance, 0.0, Strategy.BALANCED), holeCoordinate.withShot(shotNo),
                    lie, 0.0, ball, hole.geometry(), hole.cupPosition(), hole.cupPosition());
        }
        BallStrikeIntent strike = (BallStrikeIntent) intent;
        com.progolf.sim.course.Position2d aim = new com.progolf.sim.course.Position2d(strike.aimPoint().x(), strike.aimPoint().y());
        ShotDecision decision = ShotDecision.fromIntent(strike, ball.position().distanceTo(aim), policy.executionStrategyFor(lie));
        return new ShotContext(attributes, state, environment, remainingDistance, hole.zoneProfileFor(remainingDistance),
                decision, holeCoordinate.withShot(shotNo), lie, 0.0, ball, hole.geometry(), hole.cupPosition(), aim);
    }

    /**
     * Builds the exact {@link ShotContext} the resolver uses for a given shot number, from a given lie.
     * Exposed so callers and tests can reconstruct a shot identically via
     * {@link ShotResolver#resolveShot(ShotContext)}, demonstrating entry-point equivalence.
     */
    public static ShotContext buildContext(
            HoleModel hole,
            Attributes attributes,
            GolferState state,
            Environment environment,
            StrategyPolicy policy,
            double remainingDistance,
            Surface lie,
            SeedCoordinate holeCoordinate,
            int shotNo) {

        double pinLateral = hole.pinLateral();
        ShotDecision decision = policy.decide(remainingDistance, lie, pinLateral, attributes, hole.par());
        return new ShotContext(
                attributes,
                state,
                environment,
                remainingDistance,
                hole.zoneProfileFor(remainingDistance),
                decision,
                holeCoordinate.withShot(shotNo),
                lie,
                pinLateral);
    }

    /** Reconstructs a shot played from the tee box (the round's first shot). See the lie-aware overload. */
    public static ShotContext buildContext(
            HoleModel hole,
            Attributes attributes,
            GolferState state,
            Environment environment,
            StrategyPolicy policy,
            double remainingDistance,
            SeedCoordinate holeCoordinate,
            int shotNo) {
        return buildContext(hole, attributes, state, environment, policy, remainingDistance,
                Surface.TEE_BOX, holeCoordinate, shotNo);
    }
}
