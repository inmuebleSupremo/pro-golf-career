package com.progolf.sim.health;

import com.progolf.sim.core.Rng;

/**
 * The Health, Fitness &amp; Recovery engine (spec: physical-state / injury-recovery). A pure, deterministic
 * function of Physical State, age, and a supplied {@link Rng}: it seeds a golfer's fitness, accrues
 * fatigue and rolls injuries after participation, and recovers fatigue and advances rehabilitation each
 * week. Fitness lowers wear and speeds recovery; age raises wear and injury risk and slows recovery
 * (REQ-216/226). It computes only Physical State — never scores, rankings, or attributes (REQ-227).
 */
public final class HealthSystem {

    private HealthSystem() {
    }

    /** A fresh golfer's physical state: seeded fitness, no fatigue, no injury. */
    public static PhysicalState initialState(Rng rng) {
        double fitness = clamp(
                HealthConstants.FITNESS_MEAN + rng.nextGaussian() * HealthConstants.FITNESS_SPREAD,
                HealthConstants.FITNESS_MIN, HealthConstants.FITNESS_MAX);
        return PhysicalState.healthy(fitness);
    }

    /**
     * The state after competing in one event (REQ-217/219): fatigue rises (more for unfit/older golfers),
     * then — if currently uninjured — an injury may be rolled (more likely when fatigued, unfit, or older).
     */
    public static PhysicalState afterParticipation(PhysicalState state, int age, Rng rng) {
        double added = HealthConstants.FATIGUE_PER_EVENT
                * (1.0 + (1.0 - state.fitness()) * HealthConstants.FITNESS_FATIGUE_FACTOR)
                * (1.0 + ageExcess(age) * HealthConstants.AGE_FATIGUE_PER_YEAR);
        PhysicalState next = state.withFatigue(clamp01(state.fatigue() + added));

        if (state.injury().isEmpty()) {
            double chance = HealthConstants.BASE_INJURY_CHANCE
                    * (1.0 + next.fatigue() * HealthConstants.FATIGUE_INJURY_FACTOR)
                    * (1.0 + (1.0 - state.fitness()) * HealthConstants.FITNESS_INJURY_FACTOR)
                    * (1.0 + ageExcess(age) * HealthConstants.AGE_INJURY_PER_YEAR);
            if (rng.nextDouble() < chance) {
                next = next.withInjury(rollInjury(rng));
            }
        }
        return next;
    }

    /** The state after one rested week (REQ-218/220): fatigue falls and rehabilitation advances. */
    public static PhysicalState recoverWeek(PhysicalState state, int age) {
        return recoverWeek(state, age, false);
    }

    /**
     * The state after one week (REQ-218/220): fatigue falls gradually (more for fit/younger golfers). Any
     * injury advances one week of rehabilitation ONLY if the golfer did not compete this week — competing
     * while injured (playing through) freezes rehab, so grinding prolongs the injury while rest heals it
     * (spec: injury-recovery play-through). Fatigue recovery is unaffected by whether the golfer competed.
     */
    public static PhysicalState recoverWeek(PhysicalState state, int age, boolean competed) {
        double recovered = HealthConstants.RECOVERY_PER_WEEK
                * (0.5 + state.fitness() * 0.5)
                * (1.0 - ageRecoveryPenalty(age));
        PhysicalState next = state.withFatigue(clamp01(state.fatigue() - recovered));

        if (state.injury().isPresent() && !competed) {
            Injury advanced = state.injury().get().advance(1);
            next = advanced.isHealed() ? next.clearedInjury() : next.withInjury(advanced);
        }
        return next;
    }

    private static Injury rollInjury(Rng rng) {
        double sevRoll = rng.nextDouble();
        InjurySeverity severity = sevRoll < HealthConstants.SEVERITY_MINOR_CEILING
                ? InjurySeverity.MINOR
                : sevRoll < HealthConstants.SEVERITY_MODERATE_CEILING
                        ? InjurySeverity.MODERATE
                        : InjurySeverity.SEVERE;
        InjuryType[] types = InjuryType.values();
        InjuryType type = types[(int) (rng.nextDouble() * types.length)];
        return Injury.of(type, severity);
    }

    /** Years by which {@code age} exceeds the reference age (0 below it). */
    private static double ageExcess(int age) {
        return Math.max(0, age - HealthConstants.REFERENCE_AGE);
    }

    private static double ageRecoveryPenalty(int age) {
        return Math.min(HealthConstants.AGE_RECOVERY_PENALTY_CAP,
                ageExcess(age) * HealthConstants.AGE_RECOVERY_PENALTY_PER_YEAR);
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0, 1.0);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
