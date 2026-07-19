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

        // A shot played from the green is a putt: it is resolved by a dedicated make-probability model —
        // the ball rolls on the green, sheltered from wind and lie penalties, holing out near-certainly
        // from tap-in range (spec: shot-resolution putting). This is keyed on the lie (not the club) so it
        // covers long first putts too, which a distance-based club choice would clip to a full shot.
        if (context.lie() == Surface.GREEN) {
            return resolvePutt(context, rng);
        }

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

        // Mental support (psychologist) softens the effect of fatigue AND pressure on the shot
        // (spec: staff-influence); neutral at 0. Both are live conditions in world play.
        double effectiveFatigue = state.fatigue() * (1.0 - state.mentalSupport());
        double effectivePressure = state.pressure() * (1.0 - state.mentalSupport());

        double strategyMult = decision.strategy().dispersionMultiplier();
        // Situational pressure widens dispersion, resisted by Composure (spec: shot-resolution pressure);
        // neutral at 0, so calm/opening-round play is unchanged.
        double pressureMult = 1.0 + effectivePressure * (1.0 - composureNorm) * SimConstants.PRESSURE_SIGMA_WEIGHT;
        double fatigueSigmaMult = 1.0 + effectiveFatigue * SimConstants.FATIGUE_SIGMA_WEIGHT;
        // Playing through a recovering injury widens dispersion (spec: shot-resolution injury-impairment).
        // Physical — applied raw, NOT softened by mental support — and neutral at 0.
        double injurySigmaMult = 1.0 + state.injuryImpairment() * SimConstants.INJURY_SIGMA_WEIGHT;
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
        // Per-club dispersion: the driver sprays wider off the tee, a wedge is a precision club.
        double baseLateral = SimConstants.LATERAL_DISPERSION_FRACTION * shotDistance * club.lateralDispersion()
                + SimConstants.LATERAL_DISPERSION_FLOOR;
        double baseDistanceDispersion = SimConstants.DISTANCE_DISPERSION_FRACTION * shotDistance * club.distanceDispersion()
                + SimConstants.DISTANCE_DISPERSION_FLOOR;

        // Course management tightens every full shot a little — smart target selection, playing to the fat
        // side, taking the club that keeps trouble out of play (spec: shot-resolution). This is its always-on
        // value, distinct from the blow-up avoidance it already provides on the mishit tail: without it,
        // course management moved a golfer's score by essentially nothing and was not worth developing.
        double managementFactor = 1.0 + managementNorm * SimConstants.MANAGEMENT_DISPERSION_RELIEF;

        double sigmaLateral = baseLateral / lateralFactor / managementFactor
                * strategyMult * pressureMult * fatigueSigmaMult * injurySigmaMult * crossMult * lieMult * equipmentDispersion;
        // Feel (equipment) tightens distance dispersion — better proximity/touch (spec: equipment-influence);
        // neutral at 0.
        double sigmaDistance = baseDistanceDispersion / distanceFactor / managementFactor
                * strategyMult * pressureMult * fatigueSigmaMult * injurySigmaMult * lieMult * equipmentDispersion
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
        // A recovering injury also shortens carry (physical, not softened by mental support); neutral at 0.
        meanCarry *= (1.0 - state.injuryImpairment() * SimConstants.INJURY_MEAN_WEIGHT);
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
        // Distance to the pin is measured to the ACTUAL hole (pin lateral offset), not the green centre, so
        // attacking a tucked pin leaves a shorter putt while a safe centre miss leaves a longer one
        // (spec: shot-resolution pin-attacking). StrictMath for cross-platform determinism (REQ-265/299):
        // Math.hypot may vary by 1 ulp between platforms, which could flip a zone-band boundary.
        double distanceRemaining = StrictMath.hypot(longitudinalRemaining, lateral - context.pinLateral());
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

    /**
     * Resolves a putt via an explicit make-probability model (spec: shot-resolution putting). The ball
     * rolls on the green — immune to wind and lie — and either drops or finishes a short, proximity-scaled
     * distance away. Make probability falls off with distance (in feet) and rises with putting accuracy;
     * a missed putt always leaves a distinct tap-in that converges toward the hole, so the round holes out.
     */
    private static ShotOutcome resolvePutt(ShotContext context, Rng rng) {
        Attributes attr = context.attributes();
        GolferState state = context.state();
        double d = context.pinDistance(); // yards to the hole
        double accNorm = attr.norm(Attribute.PUTTING_ACCURACY);
        double proxNorm = attr.norm(Attribute.PUTTING_PROXIMITY);
        double composureNorm = attr.norm(Attribute.COMPOSURE);
        double effectiveFatigue = state.fatigue() * (1.0 - state.mentalSupport());
        double effectivePressure = state.pressure() * (1.0 - state.mentalSupport());

        // Make probability: logistic in feet, centred on a skill-raised 50%-make distance. Nerves (fatigue,
        // uncomposed pressure) shave it; mental support (psychologist) softens both.
        // Holing a putt is line AND speed: accuracy reads and starts it true, proximity (touch) rolls it the
        // right pace to drop rather than lip out. Blending proximity into the make distance gives it real
        // value in holing putts, not only in the leave — accuracy alone used to own putting outright.
        double puttSkill = SimConstants.PUTT_MAKE_ACCURACY_WEIGHT * accNorm
                + (1.0 - SimConstants.PUTT_MAKE_ACCURACY_WEIGHT) * proxNorm;
        double feet = d * SimConstants.YARDS_TO_FEET;
        double f50 = SimConstants.PUTT_MAKE_F50_BASE + SimConstants.PUTT_MAKE_F50_SPAN * puttSkill;
        double makeProbability = 1.0 / (1.0 + StrictMath.pow(feet / f50, SimConstants.PUTT_MAKE_SHARPNESS));
        makeProbability *= (1.0 - effectiveFatigue * SimConstants.PUTT_FATIGUE_PENALTY);
        makeProbability *= (1.0 - effectivePressure * (1.0 - composureNorm) * SimConstants.PUTT_PRESSURE_PENALTY);
        // A recovering injury lowers make-rate too (physical, not softened by mental support); neutral at 0.
        makeProbability *= (1.0 - state.injuryImpairment() * SimConstants.PUTT_INJURY_PENALTY);
        makeProbability = Math.min(makeProbability, SimConstants.PUTT_MAKE_CAP);

        double makeRoll = rng.nextDouble();
        double gLeave = rng.nextGaussian(); // drawn unconditionally so the stream is branch-stable
        boolean made = makeRoll < makeProbability;

        double remainingAfter;
        if (made) {
            remainingAfter = 0.0;
        } else {
            // Lag: expected leave grows with distance and shrinks with proximity; a real tap-in remains.
            double meanLeave = SimConstants.PUTT_LEAVE_FLOOR
                    + SimConstants.PUTT_LEAVE_FRACTION * d * (1.0 - SimConstants.PUTT_LEAVE_PROX_RELIEF * proxNorm);
            double leave = meanLeave * (1.0 + gLeave * SimConstants.PUTT_LEAVE_SIGMA);
            leave = Math.max(leave, SimConstants.PUTT_LEAVE_MIN);
            leave = Math.min(leave, d * SimConstants.PUTT_LEAVE_CONVERGE); // always converge toward the hole
            remainingAfter = leave;
        }

        double skill = (accNorm + proxNorm) / 2.0 - 0.5;
        FactorBreakdown factors = new FactorBreakdown(skill, 0.0, 0.0, made ? 0.5 : -0.5);
        // A putt stays on the green; carry/lateral are nominal (the round loop reads distanceRemaining).
        return new ShotOutcome(Surface.GREEN, d - remainingAfter, 0.0, remainingAfter, false, 0, 1, factors);
    }


    /** Skill compounds: dispersion is divided by an exponential in the rating (spec: shot-resolution). */
    private static double attributeFactor(Attributes attr, Attribute which) {
        return Math.exp(SimConstants.ATTRIBUTE_FACTOR_K
                * (attr.norm(which) - SimConstants.ATTRIBUTE_FACTOR_PIVOT));
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
