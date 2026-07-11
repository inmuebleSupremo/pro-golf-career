package com.progolf.sim.shot;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Rng;
import com.progolf.sim.core.RngFactory;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/**
 * The single shared shot-resolution engine (spec: shot-resolution). {@link #resolveShot(ShotContext)}
 * is the one entry point for a single shot; {@link RoundResolver} composes this exact code for AI
 * rounds, guaranteeing identical distributions across entry points (REQ-104/110/123).
 *
 * <p>Resolution follows the fixed pipeline (REQ-044): attributes, career effects, temporary modifiers,
 * and environment shape the distribution (steps 1-4); controlled randomness samples it (step 5); the
 * safety net bounds the sample (step 6); the final outcome is emitted (step 7). All randomness is drawn
 * from a generator seeded at the context's coordinate — there is no ambient randomness.
 */
public final class ShotResolver {

    private ShotResolver() {
    }

    /** Resolves exactly one shot. Rejects an incomplete decision before producing any outcome (REQ-051). */
    public static ShotOutcome resolveShot(ShotContext context) {
        Objects.requireNonNull(context, "context");
        // Guard: a shot cannot be resolved without a complete decision.
        ShotDecision decision = Objects.requireNonNull(context.decision(), "decision");
        Objects.requireNonNull(decision.club(), "club");
        Objects.requireNonNull(decision.strategy(), "strategy");

        Rng rng = RngFactory.forCoordinate(context.coordinate());
        return resolveWith(context, rng);
    }

    /**
     * Core resolution against an explicit generator. Package-private so {@link RoundResolver} shares the
     * identical per-shot code path rather than re-implementing it.
     */
    static ShotOutcome resolveWith(ShotContext context, Rng rng) {
        Attributes attr = context.attributes();
        ShotDecision decision = context.decision();
        Environment env = context.environment();
        GolferState state = context.state();
        Club club = decision.club();

        // --- Steps 1-2: base attribute factors ---
        double lateralFactor = attributeFactor(attr, club.lateralAttribute());   // higher skill -> larger -> less sigma
        double distanceFactor = attributeFactor(attr, club.distanceAttribute());
        double distanceNorm = attr.norm(club.distanceAttribute());
        double composureNorm = attr.norm(Attribute.COMPOSURE);
        double managementNorm = attr.norm(Attribute.COURSE_MANAGEMENT);

        // --- Steps 3-4: modifiers (fatigue, pressure) and environment (wind, lie) shape the distribution ---
        // Workability (equipment) improves ball-flight control in wind by raising effective wind resistance
        // (spec: equipment-influence); neutral at 0, clamped so wind is at most negated, never reversed.
        double windResist = Math.min(1.0,
                SimConstants.WIND_RESIST_FLOOR + SimConstants.WIND_RESIST_SPAN * distanceNorm
                        + state.equipmentWorkability());

        // Mental support (psychologist) softens the effect of fatigue on the shot (spec: staff-influence);
        // neutral at 0. Fatigue is the live condition in world play, so this is a meaningful in-world effect.
        double effectiveFatigue = state.fatigue() * (1.0 - state.mentalSupport());

        double strategyMult = decision.strategy().dispersionMultiplier();
        double pressureMult = 1.0 + state.pressure() * (1.0 - composureNorm) * SimConstants.PRESSURE_SIGMA_WEIGHT;
        double fatigueSigmaMult = 1.0 + effectiveFatigue * SimConstants.FATIGUE_SIGMA_WEIGHT;
        double crossMult = 1.0 + Math.abs(env.crossWind()) * SimConstants.CROSSWIND_SIGMA_WEIGHT * (1.0 - windResist);
        double lieMult = 1.0 + (1.0 - env.lieQuality()) * SimConstants.LIE_SIGMA_WEIGHT;

        // Equipment: forgiveness tightens dispersion, power extends reach (spec: equipment-influence).
        // Neutral (standard) equipment leaves both factors at 1.0, reproducing prior behaviour exactly.
        double equipmentReach = 1.0 + state.equipmentPower();
        double equipmentDispersion = 1.0 - state.equipmentForgiveness();

        // Dispersion scales with the intended shot length: a short putt is far tighter than a full drive.
        double maxReach = club.baseDistance() * (SimConstants.REACH_FLOOR + SimConstants.REACH_SPAN * distanceNorm)
                * equipmentReach;
        double shotDistance = Math.min(decision.targetDistance(), maxReach);
        double clubDispersion = club.dispersionMultiplier();
        double baseLateral = SimConstants.LATERAL_DISPERSION_FRACTION * shotDistance * clubDispersion
                + SimConstants.LATERAL_DISPERSION_FLOOR;
        double baseDistanceDispersion = SimConstants.DISTANCE_DISPERSION_FRACTION * shotDistance * clubDispersion
                + SimConstants.DISTANCE_DISPERSION_FLOOR;

        double sigmaLateral = baseLateral / lateralFactor
                * strategyMult * pressureMult * fatigueSigmaMult * crossMult * lieMult * equipmentDispersion;
        // Feel (equipment) tightens distance dispersion — better proximity/touch (spec: equipment-influence);
        // neutral at 0.
        double sigmaDistance = baseDistanceDispersion / distanceFactor
                * strategyMult * pressureMult * fatigueSigmaMult * lieMult * equipmentDispersion
                * (1.0 - state.equipmentFeel());

        // Mean carry: bounded by reachable distance; reduced by headwind and fatigue; aided by distance skill.
        double meanCarry = shotDistance;
        double headWind = env.headWind();
        if (headWind > 0) {
            meanCarry -= headWind * (1.0 - windResist) * SimConstants.HEADWIND_MEAN_WEIGHT;
        } else {
            meanCarry += -headWind * SimConstants.TAILWIND_MEAN_WEIGHT;
        }
        meanCarry *= (1.0 - effectiveFatigue * SimConstants.FATIGUE_MEAN_WEIGHT);
        if (meanCarry < 0) {
            meanCarry = 0;
        }

        // --- Step 5: controlled randomness ---
        // Rare-extreme mixture is decided first so the gaussian stream stays stable regardless of branch.
        double extremeRoll = rng.nextDouble();
        // Strategic support (caddie) further reduces mishits, alongside Course Management (spec: staff-influence);
        // neutral at 0.
        double mishitProbability = SimConstants.BASE_MISHIT_PROBABILITY
                * (1.0 - SimConstants.MISHIT_MANAGEMENT_RELIEF * managementNorm)
                * (1.0 - state.strategicSupport())
                * (1.0 + (1.0 - env.lieQuality()) * 0.5);
        double errorMultiplier = 1.0;
        double meanAdjustment = 0.0;
        if (extremeRoll < mishitProbability) {
            errorMultiplier = SimConstants.MISHIT_ERROR_MULTIPLIER;
            meanAdjustment = -meanCarry * SimConstants.MISHIT_SHORTFALL_FRACTION;
        } else if (extremeRoll > 1.0 - SimConstants.HERO_PROBABILITY) {
            errorMultiplier = SimConstants.HERO_ERROR_MULTIPLIER;
        }

        double gLateral = rng.nextGaussian();
        double gDistance = rng.nextGaussian();

        double lateral = decision.targetLateral() + gLateral * sigmaLateral * errorMultiplier;
        double carry = meanCarry + meanAdjustment + gDistance * sigmaDistance * errorMultiplier;

        // --- Step 6: safety net (bounds unrealistic samples; never floors to a good outcome) ---
        lateral = softClamp(lateral, SimConstants.SAFETY_LATERAL_CAP);
        double distanceError = softClamp(carry - meanCarry, SimConstants.SAFETY_DISTANCE_CAP);
        carry = meanCarry + distanceError;
        if (carry < 0) {
            carry = 0;
        }

        // --- Step 7: final outcome ---
        Surface surface = context.zoneProfile().surfaceAt(carry, lateral);
        double longitudinalRemaining = context.pinDistance() - carry;
        // StrictMath for cross-platform determinism (REQ-265/299): Math.hypot may vary by 1 ulp
        // between platforms, which could flip a zone-band boundary and diverge the simulation.
        double distanceRemaining = StrictMath.hypot(longitudinalRemaining, lateral);
        boolean hazard = surface.isHazard();
        int penalty = surface.penaltyStrokes();
        int strokes = 1 + penalty;

        FactorBreakdown factors = new FactorBreakdown(
                (lateralFactor + distanceFactor) / 2.0 - 1.0,               // attribute: >0 when skilled
                -((crossMult - 1.0) + (lieMult - 1.0)),                     // environment: <0 when adverse
                strategyMult - 1.0,                                         // strategy: >0 when aggressive
                -(Math.abs(gLateral) + Math.abs(gDistance)) / 2.0 + 0.8);   // luck: >0 when better than expected

        return new ShotOutcome(surface, carry, lateral, distanceRemaining, hazard, penalty, strokes, factors);
    }

    /** Maps a raw attribute to a factor in [MIN_ATTRIBUTE_FACTOR, MIN_ATTRIBUTE_FACTOR + span]; higher = better. */
    private static double attributeFactor(Attributes attr, Attribute which) {
        return SimConstants.MIN_ATTRIBUTE_FACTOR + SimConstants.ATTRIBUTE_FACTOR_SPAN * attr.norm(which);
    }

    /**
     * Soft-clamps a signed error: values within the cap pass through; the portion beyond the cap is
     * heavily compressed and then hard-limited. Poor shots remain possible; catastrophe is bounded.
     */
    private static double softClamp(double value, double cap) {
        double magnitude = Math.abs(value);
        if (magnitude <= cap) {
            return value;
        }
        double compressed = cap + (magnitude - cap) * SimConstants.SAFETY_COMPRESSION;
        double hardLimit = cap * SimConstants.SAFETY_HARD_MULTIPLE;
        compressed = Math.min(compressed, hardLimit);
        return Math.copySign(compressed, value);
    }
}
