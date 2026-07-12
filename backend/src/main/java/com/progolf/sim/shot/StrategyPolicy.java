package com.progolf.sim.shot;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.spatial.Surface;

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
                return new ShotDecision(layupClub, Math.min(layupTarget, layupClub.baseDistance()), 0.0, effective);
            }
        }

        Club club = selectClub(remainingDistance);
        double target = Math.min(remainingDistance, club.baseDistance());
        double aim = pinAttackFraction(effective, remainingDistance) * pinLateral;
        return new ShotDecision(club, target, aim, effective);
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
}
