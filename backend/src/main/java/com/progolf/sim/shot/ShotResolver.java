package com.progolf.sim.shot;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Rng;
import com.progolf.sim.core.RngFactory;
import com.progolf.sim.course.CourseGeometry;
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
            if (profile != null) {
                double rollYards = profile.rollYardsOn(contactSurface);
                if (rollYards > 0) {
                    Position2d candidate = boundedRelease(context.geometry(), flight, frame, contactPosition,
                            contactSurface, rollYards);
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
        ShotTrace trace = materializeTrace ? trace(context, settlement, roll, flight) : null;
        return new ShotOutcome(playable.lie(), raw.carry(), raw.lateral(), playable.position().distanceTo(cup),
                contactSurface.isHazard(), contactSurface.penaltyStrokes(), 1 + contactSurface.penaltyStrokes(),
                raw.factors(), settlement, raw.putt(), trace);
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
        Position2d previous = flight == null ? frame.project(-0.01, 0.0) : flight.positionAt(0.99);
        double dx = contact.x() - previous.x();
        double dy = contact.y() - previous.y();
        double length = StrictMath.hypot(dx, dy);
        if (length < 1.0e-9) {
            dx = frame.forwardX();
            dy = frame.forwardY();
            length = 1.0;
        }
        Position2d desired = contact.plus(dx / length * rollYards, dy / length * rollYards);
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

    private static ShotTrace trace(ShotContext context, ShotSettlement settlement, ShotTraceRoll roll, FlightSolution flight) {
        ShotContact contact = settlement.contact();
        ShotTraceTransition transition = settlement.recoveryKind() == RecoveryKind.NONE ? null
                : new ShotTraceTransition(settlement.recoveryKind(), contact.position(),
                        Objects.requireNonNull(settlement.recoveryPosition(), "recovery position"));
        Position2d aim = context.aimTarget();
        return new ShotTrace(context.decision().clubSpec().id(), context.ball().position(),
                new AimPoint(aim.x(), aim.y()), contact, flight == null ? java.util.List.of() : flight.samples(), roll, transition, settlement.ball().position());
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
