package com.progolf.sim.shot;

import com.progolf.sim.spatial.Surface;

/**
 * A deterministic policy that derives a shot decision from the situation — the remaining distance, the
 * ball's lie, and the pin placement. Used by {@link RoundResolver} so AI golfers produce decisions without
 * player input (REQ-117) while flowing through the same shared resolution engine.
 *
 * <p>The golfer's disposition ({@link Strategy}) sets a baseline that the situation modulates: from a
 * difficult lie they play conservatively to recover, and on a confident scoring approach an aggressive
 * disposition hunts a tucked pin (aiming toward it) while a conservative one plays the safe green centre.
 * Deterministic, so round resolution stays reproducible.
 */
public final class StrategyPolicy {

    private final Strategy strategy;

    public StrategyPolicy(Strategy strategy) {
        this.strategy = strategy;
    }

    /** Chooses a club and centre-aimed target from a clean tee-box lie at a centre pin (convenience). */
    public ShotDecision decide(double remainingDistance) {
        return decide(remainingDistance, Surface.TEE_BOX, 0.0);
    }

    /**
     * Chooses a club, target, and aim to advance the ball from {@code remainingDistance} toward the pin,
     * given the ball's {@code lie} and the pin's lateral offset. A difficult lie forces conservative play;
     * on a confident approach the golfer aims a disposition- and distance-scaled fraction toward the pin.
     */
    public ShotDecision decide(double remainingDistance, Surface lie, double pinLateral) {
        Strategy effective = effectiveStrategy(lie);
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
