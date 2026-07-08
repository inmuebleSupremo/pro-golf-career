package com.progolf.sim.shot;

/**
 * A simple deterministic policy that derives a shot decision from the remaining distance to the pin.
 * Used by {@link RoundResolver} so AI golfers produce decisions without player input (REQ-117) while
 * still flowing through the same shared resolution engine.
 *
 * <p>The policy is intentionally basic for the mathematical-core change; richer AI decision-making is a
 * later concern. It is deterministic so round resolution stays reproducible.
 */
public final class StrategyPolicy {

    private final Strategy strategy;

    public StrategyPolicy(Strategy strategy) {
        this.strategy = strategy;
    }

    /** Chooses a club and target to advance the ball from {@code remainingDistance} toward the pin. */
    public ShotDecision decide(double remainingDistance) {
        Club club = selectClub(remainingDistance);
        double target = Math.min(remainingDistance, club.baseDistance());
        return ShotDecision.straight(club, target, strategy);
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
