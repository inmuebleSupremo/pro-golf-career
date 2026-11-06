package com.progolf.sim.shot;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Rng;
import com.progolf.sim.core.RngFactory;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.GreenSurface;
import com.progolf.sim.course.GreenSurfaceGradient;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.TerrainRegion;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        return resolveWith(context, rng, false);
    }

    /** Resolves one observable shot and materializes only its already-computed canonical spatial facts. */
    public static ShotOutcome resolveShotWithTrace(ShotContext context) {
        Objects.requireNonNull(context, "context");
        ShotDecision decision = Objects.requireNonNull(context.decision(), "decision");
        Objects.requireNonNull(decision.club(), "club");
        Objects.requireNonNull(decision.strategy(), "strategy");

        Rng rng = RngFactory.forCoordinate(context.coordinate());
        return resolveWith(context, rng, true);
    }

    /**
     * Core resolution against an explicit generator. Package-private so {@link RoundResolver} shares the
     * identical per-shot code path rather than re-implementing it.
     */
    static ShotOutcome resolveWith(ShotContext context, Rng rng) {
        return resolveWith(context, rng, false);
    }

    /** Shared core for summary and observable resolution; trace mode never changes sampling or settlement. */
    static ShotOutcome resolveWith(ShotContext context, Rng rng, boolean materializeTrace) {
        Attributes attr = context.attributes();
        ShotDecision decision = context.decision();
        Environment env = context.environment();
        GolferState state = context.state();
        ClubSpec club = decision.clubSpec();

        // A shot played from the green is a putt: it is resolved by a dedicated make-probability model —
        // the ball rolls on the green, sheltered from wind and lie penalties, holing out near-certainly
        // from tap-in range (spec: shot-resolution putting). This is keyed on the lie (not the club) so it
        // covers long first putts too, which a distance-based club choice would clip to a full shot.
        if (context.lie() == Surface.GREEN
                || (context.lie() == Surface.FRINGE && decision.club() == Club.PUTTER)) {
            return resolvePutt(context, rng, materializeTrace);
        }

        ShotExecutionProfile profile = ShotExecutionProfile.derive(context.lie(), club, decision.shotFamily(), attr, env);
        ShotFamilyEligibility.Result shapeEligibility = ShotShapeEligibility.evaluate(decision.shotFamily(), decision.shotShape());
        if (!shapeEligibility.allowed()) throw new IllegalArgumentException(shapeEligibility.reason());
        ShotFrame windFrame = context.hasCanonicalGeometry()
                ? ShotFrame.toward(context.ball().position(), context.aimTarget())
                : ShotFrame.toward(new Position2d(0, 0), new Position2d(0, 1));
        double headWind = env.wind().against(windFrame);
        double crossWind = env.wind().rightward(windFrame);
        // External ball-strike entry points validate eligibility before creating this compatibility decision.
        // Legacy fixture/background decisions remain resolvable while their adapter retirement is in progress.

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
        // Wind uncertainty is distinct from its deterministic endpoint displacement below.
        double crossMult = 1.0 + Math.abs(crossWind) * SimConstants.CROSSWIND_SIGMA_WEIGHT * (1.0 - windResist);
        double lieMult = 1.0 + (1.0 - env.lieQuality()) * SimConstants.LIE_SIGMA_WEIGHT;

        // Equipment: forgiveness tightens dispersion, power extends reach (spec: equipment-influence).
        // Neutral (standard) equipment leaves both factors at 1.0, reproducing prior behaviour exactly.
        double equipmentReach = 1.0 + state.equipmentPower();
        double equipmentDispersion = 1.0 - state.equipmentForgiveness();

        // Dispersion scales with the intended shot length: a short putt is far tighter than a full drive.
        double maxReach = club.baseCarry() * (SimConstants.REACH_FLOOR + SimConstants.REACH_SPAN * distanceNorm)
                * equipmentReach * profile.effectiveCarryCapMultiplier();
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

        double shapeExecution = decision.shotShape() == ShotShape.STRAIGHT ? 1.0
                : 1.0 + SimConstants.SHAPE_EXECUTION_SIGMA_WEIGHT * (1.0 - state.equipmentWorkability())
                * (1.0 - attr.norm(club.lateralAttribute()));
        double sigmaLateral = baseLateral / lateralFactor / managementFactor
                * strategyMult * pressureMult * fatigueSigmaMult * injurySigmaMult * crossMult * lieMult * equipmentDispersion
                * profile.lateralDispersionMultiplier() * shapeExecution;
        // Feel (equipment) tightens distance dispersion — better proximity/touch (spec: equipment-influence);
        // neutral at 0.
        double sigmaDistance = baseDistanceDispersion / distanceFactor / managementFactor
                * strategyMult * pressureMult * fatigueSigmaMult * injurySigmaMult * lieMult * equipmentDispersion
                * (1.0 - state.equipmentFeel()) * profile.distanceDispersionMultiplier();

        // Mean carry: bounded by reachable distance; reduced by headwind and fatigue; aided by distance skill.
        double meanCarry = shotDistance;
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
                * (1.0 + (1.0 - env.lieQuality()) * 0.5) * profile.mishitMultiplier();
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

        // Deterministic wind drift is applied once, separately from wind uncertainty and ordinary execution error.
        double windDrift = crossWind * (1.0 - windResist) * SimConstants.CROSSWIND_DRIFT_WEIGHT;
        double lateral = decision.targetLateral() + windDrift + gLateral * sigmaLateral * errorMultiplier;
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

        return spatialize(context,
                new ShotOutcome(surface, carry, lateral, distanceRemaining, hazard, penalty, strokes, factors),
                materializeTrace, profile);
    }

    /**
     * Resolves a putt via an explicit make-probability model (spec: shot-resolution putting). The ball
     * rolls on the green — immune to wind and lie — and either drops or finishes a short, proximity-scaled
     * distance away. Make probability falls off with distance (in feet) and rises with putting accuracy;
     * a missed putt always leaves a distinct tap-in that converges toward the hole, so the round holes out.
     */
    private static ShotOutcome resolvePutt(ShotContext context, Rng rng, boolean materializeTrace) {
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
        return spatialize(context,
                new ShotOutcome(Surface.GREEN, d - remainingAfter, 0.0, remainingAfter, false, 0, 1, factors,
                        null, true), materializeTrace, null);
    }

    /** Applies canonical contact/surface/recovery after the legacy sampler has produced carry and lateral. */
    private static ShotOutcome spatialize(ShotContext context, ShotOutcome raw, boolean materializeTrace,
                                          ShotExecutionProfile profile) {
        if (!context.hasCanonicalGeometry()) {
            return raw;
        }
        BallState preShot = context.ball();
        Position2d origin = preShot.position();
        Position2d cup = context.cupPosition();
        ShotFrame frame = raw.putt()
                ? ShotFrame.toward(origin, cup)
                : ShotFrame.toward(origin, context.aimTarget());
        FlightSolution flight = raw.putt() ? null : flight(context, frame, raw, profile);
        Position2d contactPosition = flight == null ? frame.project(raw.carry(), raw.lateral()) : flight.firstContact();
        Surface contactSurface = context.geometry().surfaceAt(contactPosition);
        ShotContact contact = new ShotContact(contactPosition, contactSurface);
        ShotSettlement settlement;
        ShotTraceRoll roll = null;
        List<GroundMotionPoint> physicalGroundPath = null;
        GroundResponse ground = raw.putt() ? null : groundResponse(context, contactSurface, raw.carry(),
                backspinRpm(context, raw.carry()));
        if (contactSurface == Surface.WATER) {
            Position2d drop = waterDrop(context, contactPosition);
            if (drop == null) {
                settlement = new ShotSettlement(contact, preShot.position(), RecoveryKind.STROKE_AND_DISTANCE_FALLBACK,
                        preShot);
            } else {
                settlement = new ShotSettlement(contact, drop, RecoveryKind.WATER_DROP,
                        new BallState(drop, Surface.PRIMARY_ROUGH));
            }
        } else if (contactSurface == Surface.OUT_OF_BOUNDS) {
            settlement = new ShotSettlement(contact, preShot.position(), RecoveryKind.OUT_OF_BOUNDS_REPLAY, preShot);
        } else {
            Position2d finalPosition = contactPosition;
            if (ground != null) {
                double releaseYards = ground.totalYards();
                if (releaseYards > 0) {
                    Position2d candidate;
                    if (physicalGreenRollApplies(context, ground, flight, frame, contactPosition, contactSurface)) {
                        GreenRoll release = greenRoll(context, ground, flight, frame, contactPosition, contactSurface,
                                materializeTrace);
                        candidate = release.position();
                        physicalGroundPath = release.path();
                    } else {
                        candidate = boundedRelease(context.geometry(), flight, frame, contactPosition,
                                contactSurface, releaseYards);
                    }
                    if (!candidate.equals(contactPosition)) {
                        finalPosition = candidate;
                        roll = new ShotTraceRoll(contactPosition, candidate);
                    }
                }
            }
            settlement = new ShotSettlement(contact, null, RecoveryKind.NONE,
                    new BallState(finalPosition, context.geometry().surfaceAt(finalPosition)));
        }
        BallState playable = settlement.ball();
        ShotTrace trace = materializeTrace ? trace(context, settlement, roll, flight, ground, physicalGroundPath) : null;
        return new ShotOutcome(playable.lie(), raw.carry(), raw.lateral(), playable.position().distanceTo(cup),
                contactSurface.isHazard(), contactSurface.penaltyStrokes(), 1 + contactSurface.penaltyStrokes(),
                raw.factors(), settlement, raw.putt(), trace);
    }

    /** Fixed first contact remains the sampled carry/lateral result; only post-contact motion uses this model. */
    static GroundResponse groundResponse(ShotContext context, Surface surface, double carryYards, double spinRpm) {
        ShotFamily family = context.decision().shotFamily();
        if (!ordinaryPlayable(surface) || carryYards <= 0 || family == ShotFamily.BUNKER) {
            return new GroundResponse(spinRpm, 0, 0, 0);
        }
        LandingVelocity landing = landingVelocity(context.decision().clubSpec(), family, carryYards, spinRpm);
        double firmness = context.environment().groundFirmness();
        double spinSurfaceSpeed = spinRpm * 2 * StrictMath.PI / 60 * 0.02135;
        double spinFraction = Math.min(1, spinRpm / 9000);
        double restitution = (0.08 + 0.14 * firmness) * (1 - 0.55 * spinFraction);
        double reboundVy = Math.abs(landing.vertical()) * restitution;
        double bounceHeightYards = reboundVy * reboundVy / (2 * 9.81) * 1.0936133;
        double surfaceRetention = switch (surface) {
            case FIRST_CUT -> 0.8;
            case PRIMARY_ROUGH -> 0.55;
            case GREEN, FRINGE, FAIRWAY, TEE_BOX -> 1.0;
            default -> 0.0;
        };
        double afterImpact = Math.max(0, landing.forward() * (0.30 + 0.30 * firmness)
                * surfaceRetention - 0.45 * spinSurfaceSpeed);
        double bounceYards = afterImpact * 2 * reboundVy / 9.81 * 1.0936133;
        double rollingSpeed = Math.max(0, afterImpact * (0.33 + 0.12 * firmness)
                - 0.15 * spinSurfaceSpeed);
        if (family == ShotFamily.CHIP) {
            // A running chip skids into ground roll; limit the old chip family's distance change.
            bounceHeightYards = 0;
            bounceYards = 0;
            rollingSpeed = Math.max(0, landing.forward() * (0.50 + 0.20 * firmness)
                    * surfaceRetention - 0.20 * spinSurfaceSpeed);
        }
        double friction = rollFriction(family, firmness, surface);
        double rollYards = rollingSpeed * rollingSpeed / (2 * friction * 9.81) * 1.0936133;
        double total = bounceYards + rollYards;
        double cap = family == ShotFamily.CHIP ? Math.min(12.0, carryYards)
                : Math.min(35.0, carryYards * 0.30);
        if (total > cap) {
            double factor = cap / total;
            bounceYards *= factor;
            rollYards *= factor;
        }
        if (family == ShotFamily.CHIP) rollYards *= 0.90;
        return new GroundResponse(spinRpm, bounceHeightYards, bounceYards, rollYards);
    }

    private static double backspinRpm(ShotContext context, double carryYards) {
        ShotDecision decision = context.decision();
        if (decision.shotFamily() == ShotFamily.BUNKER || carryYards <= 0) return 0;
        if (decision.shotFamily() == ShotFamily.CHIP) {
            return Math.min(2500, 1400 * StrictMath.sqrt(carryYards / 11.6));
        }
        double base = decision.clubSpec().baseCarry();
        double spin = clubInterpolation(base, 9000, 6200, 2500)
                * StrictMath.sqrt(carryYards / (base * 0.91));
        if (decision.shotFamily() == ShotFamily.CONTROLLED) spin *= 0.90;
        if (decision.shotFamily() == ShotFamily.PITCH) spin *= 0.80;
        spin *= 0.75 + 0.25 * context.environment().lieQuality();
        spin *= switch (context.lie()) {
            case PRIMARY_ROUGH, FIRST_CUT -> 0.85;
            case DEEP_ROUGH, TREES, RECOVERY_AREA -> 0.65;
            default -> 1.0;
        };
        return Math.max(1000, Math.min(10000, spin));
    }

    private static double clubInterpolation(double baseCarry, double wedge, double iron, double driver) {
        return baseCarry <= 170 ? wedge + (baseCarry - 110) * (iron - wedge) / 60
                : iron + (baseCarry - 170) * (driver - iron) / 120;
    }

    private static LandingVelocity landingVelocity(ClubSpec club, ShotFamily family, double carryYards, double spinRpm) {
        double speed;
        double angle;
        if (family == ShotFamily.CHIP) {
            speed = 16 * StrictMath.sqrt(carryYards / 11.6);
            angle = 12;
        } else {
            speed = clubInterpolation(club.baseCarry(), 32, 46, 75)
                    * StrictMath.sqrt(carryYards / (club.baseCarry() * 0.91));
            angle = clubInterpolation(club.baseCarry(), 32, 20, 13);
            if (family == ShotFamily.CONTROLLED) angle -= 3;
        }
        speed = Math.min(85, speed);
        double radians = StrictMath.toRadians(Math.max(8, angle));
        double y = 0;
        double vx = speed * StrictMath.cos(radians);
        double vy = speed * StrictMath.sin(radians);
        for (int step = 0; step < 1000; step++) {
            double magnitude = StrictMath.hypot(vx, vy);
            double drag = 0.0017 * magnitude;
            double lift = 0.00065 * (spinRpm / 3000) * magnitude;
            double ax = -drag * vx - lift * vy;
            double ay = -9.81 - drag * vy + lift * vx;
            double priorY = y, priorVx = vx, priorVy = vy;
            y += vy * 0.02;
            vx += ax * 0.02;
            vy += ay * 0.02;
            if (y <= 0 && step > 0) {
                double fraction = priorY / (priorY - y);
                return new LandingVelocity(priorVx + (vx - priorVx) * fraction,
                        priorVy + (vy - priorVy) * fraction);
            }
        }
        throw new IllegalStateException("non-putt landing velocity did not resolve");
    }

    record GroundResponse(double spinRpm, double bounceHeightYards, double bounceYards, double rollYards) {
        double totalYards() { return bounceYards + rollYards; }
    }
    private record LandingVelocity(double forward, double vertical) { }

    private static double rollFriction(ShotFamily family, double firmness, Surface surface) {
        return (family == ShotFamily.CHIP ? 0.50 + 0.46 * (1 - firmness)
                : 0.55 + 0.54 * (1 - firmness)) * switch (surface) {
                    case GREEN -> 0.85;
                    case FRINGE, FAIRWAY, TEE_BOX -> 1.0;
                    case FIRST_CUT -> 1.2;
                    case PRIMARY_ROUGH -> 1.6;
                    default -> 1.0;
                };
    }

    /** Builds a presentation projection from existing context and settlement facts without recalculation. */
    private static FlightSolution flight(ShotContext context, ShotFrame frame, ShotOutcome raw, ShotExecutionProfile profile) {
        ShotShape shape = context.decision().shotShape();
        double sign = FlightSolution.shapeCurveSign(context.handedness(), shape);
        double curve = sign * raw.carry() * SimConstants.SHAPE_CURVE_FRACTION;
        double apex = raw.carry() * (profile == null ? SimConstants.FLIGHT_APEX_FRACTION
                : SimConstants.FLIGHT_APEX_FRACTION * (context.decision().shotFamily() == ShotFamily.PITCH ? 0.7 : 1.0));
        return new FlightSolution(frame, raw.carry(), raw.lateral(), curve, apex);
    }

    /**
     * Resolves one authoritative post-contact ground segment. It may cross exactly one ordinary playable
     * surface boundary; a second boundary or non-playable terrain clamps the endpoint before that boundary.
     * This is intentionally not general terrain traversal or roll-created hazard settlement.
     */
    private static Position2d boundedRelease(CourseGeometry geometry, FlightSolution flight, ShotFrame frame,
                                             Position2d contact, Surface contactSurface, double rollYards) {
        if (!ordinaryPlayable(contactSurface)) return contact;
        Position2d direction = releaseDirection(flight, frame, contact);
        Position2d desired = contact.plus(direction.x() * rollYards, direction.y() * rollYards);
        List<Double> boundaries = boundaryParameters(geometry, contact, desired);
        boundaries.add(0.0);
        boundaries.add(1.0);
        boundaries.sort(Comparator.naturalOrder());

        Surface current = contactSurface;
        int transitions = 0;
        for (int i = 0; i < boundaries.size() - 1; i++) {
            double start = boundaries.get(i);
            double end = boundaries.get(i + 1);
            if (end - start < 1.0e-9) continue;
            Surface next = geometry.surfaceAt(interpolate(contact, desired, (start + end) / 2.0));
            if (next == current) continue;
            if (!ordinaryPlayable(next) || transitions == 1) {
                return pointBefore(contact, desired, start);
            }
            transitions++;
            current = next;
        }
        return desired;
    }

    private static Position2d releaseDirection(FlightSolution flight, ShotFrame frame, Position2d contact) {
        Position2d previous = flight == null ? frame.project(-0.01, 0.0) : flight.positionAt(0.99);
        double dx = contact.x() - previous.x();
        double dy = contact.y() - previous.y();
        double length = StrictMath.hypot(dx, dy);
        if (length < 1.0e-9) {
            dx = frame.forwardX();
            dy = frame.forwardY();
            length = 1.0;
        }
        return new Position2d(dx / length, dy / length);
    }

    /** Only a physical chip/pitch whose existing release can touch the exact green footprint uses slope roll. */
    private static boolean physicalGreenRollApplies(ShotContext context, GroundResponse ground, FlightSolution flight,
                                                     ShotFrame frame, Position2d contact, Surface contactSurface) {
        GreenSurface surface = context.greenSurface();
        ShotFamily family = context.decision().shotFamily();
        if (surface == null || (family != ShotFamily.CHIP && family != ShotFamily.PITCH)
                || ground.rollYards() <= 0 || !ordinaryPlayable(contactSurface)) return false;
        boolean matchingGreen = context.geometry().regions().stream()
                .anyMatch(region -> region.surface() == Surface.GREEN && region.boundary().equals(surface.footprint()));
        if (!matchingGreen) return false;
        if (contactSurface == Surface.GREEN) return true;
        Position2d direction = releaseDirection(flight, frame, contact);
        Position2d endpoint = contact.plus(direction.x() * ground.totalYards(), direction.y() * ground.totalYards());
        List<Double> crossings = new ArrayList<>();
        addBoundaryParameters(crossings, surface.footprint(), contact, endpoint);
        return !crossings.isEmpty();
    }

    /** Reuses the spin/cap-adjusted roll distance as energy, then resolves its two-dimensional path. */
    private static GreenRoll greenRoll(ShotContext context, GroundResponse ground, FlightSolution flight,
                                       ShotFrame frame, Position2d contact, Surface contactSurface, boolean trace) {
        CourseGeometry geometry = context.geometry();
        ShotFamily family = context.decision().shotFamily();
        double firmness = context.environment().groundFirmness();
        Position2d direction = releaseDirection(flight, frame, contact);
        List<GroundMotionPoint> path = trace ? new ArrayList<>() : null;
        Position2d bounceLanding = ground.bounceYards() > 0
                ? boundedRelease(geometry, flight, frame, contact, contactSurface, ground.bounceYards()) : contact;
        double actualBounce = contact.distanceTo(bounceLanding);
        if (path != null && ground.bounceHeightYards() > 0.0 && actualBounce > 0.01) {
            path.add(new GroundMotionPoint(GroundMotionPoint.Kind.BOUNCE_APEX,
                    contact.plus(direction.x() * actualBounce / 2, direction.y() * actualBounce / 2),
                    ground.bounceHeightYards()));
            path.add(new GroundMotionPoint(GroundMotionPoint.Kind.BOUNCE_LANDING, bounceLanding, 0));
        }
        if (actualBounce < ground.bounceYards() - 1.0e-6) {
            return finishGreenRoll(bounceLanding, path, true);
        }

        double initialDeceleration = rollingDeceleration(family, firmness, contactSurface);
        double speed = StrictMath.sqrt(2 * initialDeceleration * ground.rollYards());
        double velocityX = direction.x() * speed, velocityY = direction.y() * speed;
        Position2d position = bounceLanding;
        Position2d lastSample = position;
        double sampleSpacing = Math.max(0.35, Math.min(2.0, ground.rollYards() / 8));
        for (int step = 0; step < 1000; step++) {
            speed = StrictMath.hypot(velocityX, velocityY);
            if (speed <= 0.01) return finishGreenRoll(position, path, false);
            Surface terrain = geometry.surfaceAt(position);
            double resistance = rollingDeceleration(family, firmness, terrain);
            GreenSurfaceGradient grade = terrain == Surface.GREEN
                    ? context.greenSurface().gradientAt(position) : new GreenSurfaceGradient(0, 0);
            double accelerationX = -GROUND_GRAVITY_YARDS_PER_SECOND_SQUARED * grade.x() - resistance * velocityX / speed;
            double accelerationY = -GROUND_GRAVITY_YARDS_PER_SECOND_SQUARED * grade.y() - resistance * velocityY / speed;
            double timeStep = Math.min(0.02, 0.1 / speed);
            double nextVelocityX = velocityX + accelerationX * timeStep;
            double nextVelocityY = velocityY + accelerationY * timeStep;
            if (nextVelocityX * velocityX + nextVelocityY * velocityY <= 0) {
                return finishGreenRoll(position, path, false);
            }
            Position2d next = position.plus((velocityX + nextVelocityX) * timeStep / 2,
                    (velocityY + nextVelocityY) * timeStep / 2);
            BoundaryTraversal boundary = traverseRollStep(geometry, position, next, terrain);
            if (boundary.stopped()) return finishGreenRoll(boundary.position(), path, true);
            position = next;
            velocityX = nextVelocityX;
            velocityY = nextVelocityY;
            if (path != null && position.distanceTo(lastSample) >= sampleSpacing) {
                path.add(new GroundMotionPoint(GroundMotionPoint.Kind.ROLL_SAMPLE, position, 0));
                lastSample = position;
            }
        }
        return finishGreenRoll(position, path, false);
    }

    private static final double GROUND_GRAVITY_YARDS_PER_SECOND_SQUARED = 9.81 * 1.0936133;

    private static double rollingDeceleration(ShotFamily family, double firmness, Surface terrain) {
        return rollFriction(family, firmness, terrain) * GROUND_GRAVITY_YARDS_PER_SECOND_SQUARED;
    }

    private static BoundaryTraversal traverseRollStep(CourseGeometry geometry, Position2d from, Position2d to,
                                                        Surface current) {
        List<Double> boundaries = boundaryParameters(geometry, from, to);
        if (boundaries.isEmpty() && geometry.surfaceAt(to) == current) return new BoundaryTraversal(to, false);
        boundaries.add(1.0);
        double start = 0.0;
        for (double end : boundaries) {
            if (end - start < 1.0e-9) continue;
            Surface next = geometry.surfaceAt(interpolate(from, to, (start + end) / 2));
            if (next != current) {
                if (!ordinaryPlayable(next)) {
                    return new BoundaryTraversal(pointBefore(from, to, start), true);
                }
                current = next;
            }
            start = end;
        }
        return new BoundaryTraversal(to, false);
    }

    private static GreenRoll finishGreenRoll(Position2d position, List<GroundMotionPoint> path, boolean boundaryStop) {
        if (path == null) return new GreenRoll(position, List.of());
        path.add(new GroundMotionPoint(boundaryStop ? GroundMotionPoint.Kind.BOUNDARY_STOP
                : GroundMotionPoint.Kind.ROLL_STOP, position, 0));
        return new GreenRoll(position, List.copyOf(path));
    }

    private record GreenRoll(Position2d position, List<GroundMotionPoint> path) { }
    private record BoundaryTraversal(Position2d position, boolean stopped) { }

    private static boolean ordinaryPlayable(Surface surface) {
        return switch (surface) {
            case TEE_BOX, FAIRWAY, FIRST_CUT, PRIMARY_ROUGH, DEEP_ROUGH, GREEN, FRINGE, WASTE_AREA -> true;
            case BUNKER, RECOVERY_AREA, TREES, WATER, OUT_OF_BOUNDS -> false;
        };
    }

    private static List<Double> boundaryParameters(CourseGeometry geometry, Position2d from, Position2d to) {
        List<Double> result = new ArrayList<>();
        addBoundaryParameters(result, geometry.playableBoundary(), from, to);
        for (TerrainRegion region : geometry.regions()) addBoundaryParameters(result, region.boundary(), from, to);
        result.sort(Comparator.naturalOrder());
        List<Double> distinct = new ArrayList<>();
        for (double parameter : result) {
            if (parameter > 1.0e-9 && parameter < 1.0 - 1.0e-9
                    && (distinct.isEmpty() || parameter - distinct.getLast() > 1.0e-8)) {
                distinct.add(parameter);
            }
        }
        return distinct;
    }

    private static void addBoundaryParameters(List<Double> parameters, List<Position2d> polygon,
                                              Position2d from, Position2d to) {
        for (int i = 0; i < polygon.size(); i++) {
            double parameter = segmentIntersectionParameter(from, to, polygon.get(i), polygon.get((i + 1) % polygon.size()));
            if (Double.isFinite(parameter)) parameters.add(parameter);
        }
    }

    private static double segmentIntersectionParameter(Position2d from, Position2d to, Position2d edgeStart, Position2d edgeEnd) {
        double rx = to.x() - from.x();
        double ry = to.y() - from.y();
        double sx = edgeEnd.x() - edgeStart.x();
        double sy = edgeEnd.y() - edgeStart.y();
        double denominator = rx * sy - ry * sx;
        if (Math.abs(denominator) < 1.0e-12) return Double.NaN;
        double qpx = edgeStart.x() - from.x();
        double qpy = edgeStart.y() - from.y();
        double t = (qpx * sy - qpy * sx) / denominator;
        double u = (qpx * ry - qpy * rx) / denominator;
        return t >= 0.0 && t <= 1.0 && u >= 0.0 && u <= 1.0 ? t : Double.NaN;
    }

    private static Position2d pointBefore(Position2d from, Position2d to, double boundaryParameter) {
        double distance = from.distanceTo(to);
        double margin = distance == 0.0 ? 0.0 : Math.min(0.01 / distance, boundaryParameter / 2.0);
        return interpolate(from, to, Math.max(0.0, boundaryParameter - margin));
    }

    private static Position2d interpolate(Position2d from, Position2d to, double parameter) {
        return new Position2d(from.x() + (to.x() - from.x()) * parameter,
                from.y() + (to.y() - from.y()) * parameter);
    }

    private static ShotTrace trace(ShotContext context, ShotSettlement settlement, ShotTraceRoll roll,
                                   FlightSolution flight, GroundResponse ground, List<GroundMotionPoint> physicalGroundPath) {
        ShotContact contact = settlement.contact();
        ShotTraceTransition transition = settlement.recoveryKind() == RecoveryKind.NONE ? null
                : new ShotTraceTransition(settlement.recoveryKind(), contact.position(),
                        Objects.requireNonNull(settlement.recoveryPosition(), "recovery position"));
        Position2d aim = context.aimTarget();
        return new ShotTrace(context.decision().clubSpec().id(), context.ball().position(),
                new AimPoint(aim.x(), aim.y()), contact, flight == null ? java.util.List.of() : flight.samples(),
                roll, transition, settlement.ball().position(), ground == null ? 0 : ground.spinRpm(),
                physicalGroundPath == null ? groundPath(contact.position(), roll, ground) : physicalGroundPath);
    }

    private static List<GroundMotionPoint> groundPath(Position2d contact, ShotTraceRoll release, GroundResponse ground) {
        if (release == null || ground == null) return List.of();
        Position2d finalPoint = release.to();
        double actual = contact.distanceTo(finalPoint);
        double plannedHop = ground.bounceYards();
        List<GroundMotionPoint> path = new ArrayList<>();
        if (plannedHop > 0.01 && actual >= plannedHop - 1.0e-6) {
            double hopFraction = Math.min(1, plannedHop / actual);
            path.add(new GroundMotionPoint(GroundMotionPoint.Kind.BOUNCE_APEX,
                    interpolate(contact, finalPoint, hopFraction / 2), ground.bounceHeightYards()));
            path.add(new GroundMotionPoint(GroundMotionPoint.Kind.BOUNCE_LANDING,
                    interpolate(contact, finalPoint, hopFraction), 0));
        }
        path.add(new GroundMotionPoint(actual < ground.totalYards() - 1.0e-6
                ? GroundMotionPoint.Kind.BOUNDARY_STOP : GroundMotionPoint.Kind.ROLL_STOP, finalPoint, 0));
        return List.copyOf(path);
    }

    /** Implements the specified tee-ward 15-yard start and 1-yard deterministic rough scan. */
    private static Position2d waterDrop(ShotContext context, Position2d contact) {
        Position2d pre = context.ball().position();
        Position2d cup = context.cupPosition();
        double dx = pre.x() - contact.x();
        double dy = pre.y() - contact.y();
        double distance = StrictMath.hypot(dx, dy);
        if (distance < SimConstants.WATER_DROP_SETBACK) {
            return null;
        }
        double ux = dx / distance;
        double uy = dy / distance;
        double preDistance = pre.distanceTo(cup);
        for (double setback = SimConstants.WATER_DROP_SETBACK; setback <= distance; setback += 1.0) {
            Position2d candidate = contact.plus(ux * setback, uy * setback);
            if (context.geometry().surfaceAt(candidate) == Surface.PRIMARY_ROUGH
                    && candidate.distanceTo(cup) <= preDistance) {
                return candidate;
            }
        }
        return null;
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
