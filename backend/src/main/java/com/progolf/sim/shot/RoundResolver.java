package com.progolf.sim.shot;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
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

        for (int shotNo = 1; shotNo <= SimConstants.MAX_SHOTS_PER_HOLE; shotNo++) {
            ShotContext context = buildContext(hole, attributes, state, environment, policy, remaining, holeCoordinate, shotNo);
            ShotOutcome outcome = ShotResolver.resolveShot(context);
            shots.add(outcome);
            totalStrokes += outcome.strokes();
            remaining = outcome.distanceRemaining();
            if (remaining <= SimConstants.HOLED_THRESHOLD) {
                break;
            }
        }

        return new RoundOutcome(shots, totalStrokes);
    }

    /**
     * Builds the exact {@link ShotContext} the resolver uses for a given shot number. Exposed so callers
     * and tests can reconstruct a shot identically via {@link ShotResolver#resolveShot(ShotContext)},
     * demonstrating entry-point equivalence.
     */
    public static ShotContext buildContext(
            HoleModel hole,
            Attributes attributes,
            GolferState state,
            Environment environment,
            StrategyPolicy policy,
            double remainingDistance,
            SeedCoordinate holeCoordinate,
            int shotNo) {

        ShotDecision decision = policy.decide(remainingDistance);
        return new ShotContext(
                attributes,
                state,
                environment,
                remainingDistance,
                hole.zoneProfileFor(remainingDistance),
                decision,
                holeCoordinate.withShot(shotNo));
    }
}
