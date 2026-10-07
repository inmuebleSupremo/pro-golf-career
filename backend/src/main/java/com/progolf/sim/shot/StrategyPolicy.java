package com.progolf.sim.shot;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.course.Position2d;

/**
 * A deterministic policy that derives a shot decision from the situation — the remaining distance, the
 * ball's lie, and the pin placement. Used by {@link RoundResolver} so AI golfers produce decisions without
 * player input (REQ-117) while flowing through the same shared resolution engine.
 *
 * <p>The golfer's disposition ({@link Strategy}) sets a baseline that the situation modulates: from a
 * difficult lie they play conservatively to recover; on a long reachable approach an aggressive or
 * long-hitting golfer goes for the green while a conservative one lays up to a full wedge; and on a
 * confident scoring approach an aggressive disposition hunts a tucked pin while a conservative one plays
 * the safe green centre. Deterministic, so round resolution stays reproducible.
 */
public final class StrategyPolicy {

    /** Neutral attributes used when a caller does not supply them (aim-only decisions in tests). */
    private static final Attributes NEUTRAL = Attributes.uniform(50);

    private final Strategy strategy;

    public StrategyPolicy(Strategy strategy) {
        this.strategy = strategy;
    }

    /** Strategy used by this compatibility policy; route semantics remain independent of it. */
    public Strategy strategy() {
        return strategy;
    }

    /** Internal AI-only risk posture retained by the transitional compatibility sampler. */
    public Strategy executionStrategyFor(Surface lie) {
        return effectiveStrategy(lie);
    }

    /** Chooses a club and centre-aimed target from a clean tee-box lie at a centre pin (convenience). */
    public ShotDecision decide(double remainingDistance) {
        return decide(remainingDistance, Surface.TEE_BOX, 0.0, NEUTRAL, 4);
    }

    /** Situational decision without the golfer's attributes/par (neutral reach, par 4 so no lay-up; aim callers). */
    public ShotDecision decide(double remainingDistance, Surface lie, double pinLateral) {
        return decide(remainingDistance, lie, pinLateral, NEUTRAL, 4);
    }

    /**
     * Chooses a club, target, and aim to advance the ball from {@code remainingDistance} toward the pin,
     * given the ball's {@code lie}, the pin's lateral offset, the golfer's {@code attributes}, and the
     * hole's {@code par}. A difficult lie forces conservative recovery; a long par-5 approach is a
     * lay-up-or-go decision; a confident scoring approach aims a disposition- and distance-scaled fraction
     * toward the pin.
     */
    public ShotDecision decide(double remainingDistance, Surface lie, double pinLateral, Attributes attributes, int par) {
        PolicyPlan plan = plan(remainingDistance, lie, pinLateral, attributes, par);
        return new ShotDecision(plan.club(), plan.targetDistance(), plan.targetLateral(), plan.strategy());
    }

    private PolicyPlan plan(double remainingDistance, Surface lie, double pinLateral, Attributes attributes, int par) {
        Strategy effective = effectiveStrategy(lie);

        // Lay up vs go for it: only on a long PAR-5 approach (a par 4/3 has no stroke to spare, and a tee
        // shot always goes). Go for it when aggressive or the green is comfortably reachable (playing to a
        // long hitter's strength); otherwise lay up to a full wedge, aimed safely at the centre.
        if (par >= 5 && lie != Surface.TEE_BOX && remainingDistance >= SimConstants.LAYUP_MIN_DISTANCE) {
            Club reachClub = selectClub(remainingDistance);
            double maxReach = reachClub.baseDistance() * (SimConstants.REACH_FLOOR
                    + SimConstants.REACH_SPAN * attributes.norm(reachClub.distanceAttribute()));
            boolean comfortablyReachable = maxReach >= remainingDistance + SimConstants.LAYUP_COMFORTABLE_MARGIN;
            // An aggressive golfer always fires at the green; anyone who can reach comfortably (a long
            // hitter's strength) also goes; otherwise the golfer lays up to a safe full wedge.
            boolean goForIt = effective == Strategy.AGGRESSIVE || comfortablyReachable;
            if (!goForIt) {
                double layupTarget = remainingDistance - SimConstants.LAYUP_LEAVE_DISTANCE;
                Club layupClub = selectClub(layupTarget);
                return new PolicyPlan(layupClub, Math.min(layupTarget, layupClub.baseDistance()), 0.0, effective);
            }
        }

        Club club = selectClub(remainingDistance);
        double target = Math.min(remainingDistance, club.baseDistance());
        double aim = pinAttackFraction(effective, remainingDistance) * pinLateral;
        return new PolicyPlan(club, target, aim, effective);
    }

    /**
     * AI policy's public decision output. Disposition remains policy-only; execution receives the same
     * individual-club and canonical-point contract as human play.
     */
    public ShotIntent decideShotIntent(HoleModel hole, BallState ball, double remainingDistance, Surface lie,
                                       Attributes attributes, int par) {
        if (lie == Surface.GREEN || (lie == Surface.FRINGE && remainingDistance <= 10.0)) {
            return new PuttIntent();
        }
        return decideIntent(hole, ball, remainingDistance, lie, attributes, par);
    }

    /** Produces the non-putting branch of the shared intent contract. */
    public BallStrikeIntent decideIntent(HoleModel hole, BallState ball, double remainingDistance, Surface lie,
                                         Attributes attributes, int par) {
        ShotAim.Reference aim = ShotAim.forBall(hole, ball, strategy);
        PolicyPlan plan = plan(remainingDistance, lie, aim.pinLateral(), attributes, par);
        ShotFamily family = familyFor(remainingDistance, lie, par);
        ClubSpec base = clubFor(family, plan.club());
        double intendedCarry = family == ShotFamily.BUNKER
                ? Math.min(plan.targetDistance(), normalReach(base, attributes))
                : plan.targetDistance();
        // The intended point is first contact / landing, not the desired final resting point.
        Position2d route = ShotFrame.toward(ball.position(), aim.target())
                .project(intendedCarry, plan.targetLateral());
        return new BallStrikeIntent(base.id(), new AimPoint(route.x(), route.y()), family);
    }

    private ShotFamily familyFor(double remainingDistance, Surface lie, int par) {
        if (lie == Surface.BUNKER) return ShotFamily.BUNKER;
        if (remainingDistance <= 8.0 && lie != Surface.TEE_BOX) return ShotFamily.CHIP;
        if (remainingDistance <= 20.0 && lie != Surface.TEE_BOX) return ShotFamily.PITCH;
        if (par >= 5 && lie != Surface.TEE_BOX && remainingDistance >= SimConstants.LAYUP_MIN_DISTANCE
                && strategy == Strategy.CONSERVATIVE) return ShotFamily.CONTROLLED;
        return ShotFamily.FULL;
    }

    private static ClubSpec clubFor(ShotFamily family, Club legacy) {
        return switch (family) {
            case BUNKER -> ClubSpec.of(ClubId.SAND_WEDGE);
            case PITCH -> ClubSpec.of(ClubId.GAP_WEDGE);
            case CHIP -> ClubSpec.of(ClubId.EIGHT_IRON);
            case FULL, CONTROLLED -> {
                ClubSpec candidate = ClubSpec.forLegacy(legacy);
                yield candidate.family() == Club.PUTTER ? ClubSpec.of(ClubId.GAP_WEDGE) : candidate;
            }
        };
    }

    private static double normalReach(ClubSpec club, Attributes attributes) {
        return club.baseCarry() * (SimConstants.REACH_FLOOR
                + SimConstants.REACH_SPAN * attributes.norm(club.distanceAttribute()));
    }

    /** From a difficult lie (deep rough / bunker / recovery / trees) the golfer plays conservatively. */
    private Strategy effectiveStrategy(Surface lie) {
        return lie.recoveryDifficulty() >= SimConstants.RECOVERY_CAUTION_THRESHOLD ? Strategy.CONSERVATIVE : strategy;
    }

    /** Fraction of the pin offset to aim at: the strategy's pin-attack scaled by shot confidence (distance). */
    private static double pinAttackFraction(Strategy strategy, double remainingDistance) {
        double span = SimConstants.PIN_ATTACK_FADE_FAR - SimConstants.PIN_ATTACK_FADE_NEAR;
        double confidence = (SimConstants.PIN_ATTACK_FADE_FAR - remainingDistance) / span;
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        return strategy.pinAttack() * confidence;
    }

    private static Club selectClub(double remainingDistance) {
        if (remainingDistance > 260) {
            return Club.DRIVER;
        } else if (remainingDistance > 210) {
            return Club.FAIRWAY_WOOD;
        } else if (remainingDistance > 185) {
            return Club.HYBRID;
        } else if (remainingDistance > 40) {
            return Club.IRON;
        } else if (remainingDistance > 8) {
            return Club.WEDGE;
        } else {
            return Club.PUTTER;
        }
    }

    /** AI-only planning data; converted to an intent before the shared execution boundary. */
    private record PolicyPlan(Club club, double targetDistance, double targetLateral, Strategy strategy) { }
}
