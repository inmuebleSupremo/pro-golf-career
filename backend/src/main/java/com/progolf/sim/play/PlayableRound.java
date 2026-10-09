package com.progolf.sim.play;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.AimEnvelope;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.PuttIntent;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.Club;
import com.progolf.sim.shot.ClubSpec;
import com.progolf.sim.shot.ShotGuidance;
import com.progolf.sim.shot.ShotFamilyEligibility;
import com.progolf.sim.shot.HoleStats;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.ShotContext;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotFrame;
import com.progolf.sim.shot.ShotAim;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.ShotResolver;
import com.progolf.sim.shot.ShotStatLine;
import com.progolf.sim.shot.SimConstants;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.shot.StrategyPolicy;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.core.Handedness;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An interactive, stateful 18-hole round a human plays shot-by-shot through the shot engine (spec:
 * playable-round). Each shot the player sees the {@link #situation()} and submits a {@link ShotDecision}
 * (club / target / risk) to {@link #playShot}; the round is always skippable via {@link #simShot} /
 * {@link #simHole} / {@link #simRound}, which use the same {@link StrategyPolicy} the AI does.
 *
 * <p>The per-shot loop mirrors {@code RoundResolver.resolveHole} exactly — the same seed coordinates,
 * penalty-hazard recovery (water-drop vs out-of-bounds stroke-and-distance), holed threshold, and shot
 * cap — so a fully-simmed round is identical to the
 * automatic round resolution for the same inputs and seed, and playing by hand differs only by the human's
 * decisions. Framework-free and deterministic given the decisions and seed.
 */
public final class PlayableRound {

    private static final int HOLES = 18;

    private final Attributes attributes;
    private final GolferState state;
    private final List<HoleToPlay> holes;
    private final SeedCoordinate base;
    private final StrategyPolicy simPolicy;
    private final Handedness handedness;

    private int holeIndex;        // 0-based; == HOLES when the round is complete
    private int shotNumber = 1;   // 1-based within the current hole
    private double remaining;     // distance to the pin for the current shot
    private int strokesThisHole;
    private int totalStrokes;
    private Surface lie = Surface.TEE_BOX;
    private BallState ball;
    private final List<Integer> holeScores = new ArrayList<>();
    private final List<ShotOutcome> currentHoleShots = new ArrayList<>();
    private final List<PlayedHole> playedHoles = new ArrayList<>();
    private ShotStatLine shotStats = ShotStatLine.empty();

    public PlayableRound(Attributes attributes, GolferState state, List<HoleToPlay> holes,
                         SeedCoordinate base, Strategy simStrategy) {
        this(attributes, state, holes, base, simStrategy, Handedness.RIGHT);
    }
    public PlayableRound(Attributes attributes, GolferState state, List<HoleToPlay> holes,
                         SeedCoordinate base, Strategy simStrategy, Handedness handedness) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.state = Objects.requireNonNull(state, "state");
        Objects.requireNonNull(holes, "holes");
        if (holes.size() != HOLES) {
            throw new IllegalArgumentException("a round is " + HOLES + " holes: " + holes.size());
        }
        this.holes = List.copyOf(holes);
        this.base = Objects.requireNonNull(base, "base");
        this.simPolicy = new StrategyPolicy(Objects.requireNonNull(simStrategy, "simStrategy"));
        this.handedness = handedness == null ? Handedness.RIGHT : handedness;
        this.remaining = this.holes.get(0).model().startDistance();
        this.ball = initialBall(this.holes.get(0));
    }

    /** Whether all eighteen holes have been played. */
    public boolean isComplete() {
        return holeIndex >= HOLES;
    }

    /** The current hole number (1-based). */
    public int currentHole() {
        return holeIndex + 1;
    }

    /** The exact setup-specific model currently used to resolve the active hole. */
    public HoleModel currentHoleModel() {
        requireNotComplete();
        return holes.get(holeIndex).model();
    }

    /** The situation for the current shot. */
    public ShotSituation situation() {
        requireNotComplete();
        HoleToPlay hole = holes.get(holeIndex);
        if (ball == null || hole.model().geometry() == null) {
            return new ShotSituation(holeIndex + 1, hole.par(), shotNumber, strokesThisHole,
                    remaining, lie, hole.model().pinLateral(), hole.model().zoneProfileFor(remaining));
        }
        return new ShotSituation(holeIndex + 1, hole.par(), shotNumber, strokesThisHole,
                remaining, lie, hole.model().pinLateral(), hole.model().zoneProfileFor(remaining),
                revision(), envelope(hole.model().geometry()), guidance(hole.model(), hole.environment()));
    }

    /** Plays the current shot with the human's decision (club / target / risk). */
    public ShotOutcome playShot(ShotDecision decision) {
        requireNotComplete();
        return resolveOne(Objects.requireNonNull(decision, "decision"), null, true);
    }

    /** Plays a human-owned spatial intent. The submitted point, not route policy, chooses the shot frame. */
    public ShotOutcome playShot(BallStrikeIntent intent) {
        requireNotComplete();
        Objects.requireNonNull(intent, "intent");
        if (ball == null || holes.get(holeIndex).model().geometry() == null) {
            throw new IllegalStateException("spatial intent requires canonical hole geometry");
        }
        Position2d target = new Position2d(intent.aimPoint().x(), intent.aimPoint().y());
        validateAim(target, holes.get(holeIndex).model().geometry());
        ShotFamilyEligibility.Result eligibility = ShotFamilyEligibility.evaluate(lie, ClubSpec.of(intent.club()), intent.shotFamily());
        if (!eligibility.allowed()) throw new IllegalArgumentException(eligibility.reason());
        double requestedCarry = ball.position().distanceTo(target);
        return resolveOne(ShotDecision.fromIntent(intent, requestedCarry, Strategy.BALANCED), target, true);
    }

    /** Deliberate entry point for the existing non-spatial putting model. */
    public ShotOutcome playPutt(PuttIntent intent) {
        requireNotComplete();
        Objects.requireNonNull(intent, "intent");
        if (lie != Surface.GREEN && lie != Surface.FRINGE) throw new IllegalArgumentException("putting is available only on green or fringe");
        return resolveOne(puttDecision(), ball == null ? null : holes.get(holeIndex).model().cupPosition(), true);
    }

    /** Sims the current shot with the automatic policy. */
    public ShotOutcome simShot() {
        requireNotComplete();
        SimShot shot = simShotPlan();
        return resolveOne(shot.decision(), shot.aimTarget(), true);
    }

    /** Sims the rest of the current hole with the automatic policy. */
    public void simHole() {
        requireNotComplete();
        int hole = holeIndex;
        while (!isComplete() && holeIndex == hole) {
            SimShot shot = simShotPlan();
            resolveOne(shot.decision(), shot.aimTarget(), false);
        }
    }

    /** Sims the rest of the round with the automatic policy. */
    public void simRound() {
        while (!isComplete()) {
            SimShot shot = simShotPlan();
            resolveOne(shot.decision(), shot.aimTarget(), false);
        }
    }

    /** The automatic policy's decision for the current situation (lie + pin + attributes), matching the AI path. */
    private SimShot simShotPlan() {
        HoleToPlay hole = holes.get(holeIndex);
        if (ball != null && hole.model().geometry() != null) {
            com.progolf.sim.shot.ShotIntent selected = simPolicy.decideShotIntent(hole.model(), ball, remaining, lie,
                    attributes, hole.par(), hole.environment());
            if (selected instanceof PuttIntent) {
                return new SimShot(puttDecision(), hole.model().cupPosition());
            }
            BallStrikeIntent intent = (BallStrikeIntent) selected;
            Position2d aim = new Position2d(intent.aimPoint().x(), intent.aimPoint().y());
            return new SimShot(ShotDecision.fromIntent(intent, ball.position().distanceTo(aim),
                    simPolicy.executionStrategyFor(lie)), aim);
        }
        return new SimShot(simPolicy.decide(remaining, lie, hole.model().pinLateral(), attributes, hole.par()), null);
    }

    private ShotDecision puttDecision() {
        return new ShotDecision(Club.PUTTER, remaining, 0.0, Strategy.BALANCED, ClubSpec.of(ClubId.PUTTER));
    }

    private ShotOutcome resolveOne(ShotDecision decision, Position2d humanAimTarget, boolean materializeTrace) {
        HoleToPlay hole = holes.get(holeIndex);
        double preShotRemaining = remaining;
        SeedCoordinate coord = base.withHole(holeIndex + 1).withShot(shotNumber);
        ShotContext context = contextFor(hole, decision, coord, humanAimTarget);
        ShotOutcome outcome = materializeTrace ? ShotResolver.resolveShotWithTrace(context) : ShotResolver.resolveShot(context);

        totalStrokes += outcome.strokes();
        strokesThisHole += outcome.strokes();
        currentHoleShots.add(outcome);
        lie = outcome.finalSurface();

        boolean holed;
        if (outcome.settlement() != null) {
            ball = outcome.settlement().ball();
            lie = ball.lie();
            remaining = ball.position().distanceTo(hole.model().cupPosition());
            holed = !outcome.hazardEntered() && remaining <= SimConstants.HOLED_THRESHOLD;
        } else if (outcome.hazardEntered()) {
            // Penalty-hazard recovery, identical to RoundResolver so simmed == auto (spec: shot-resolution).
            if (outcome.finalSurface() == Surface.WATER) {
                remaining = Math.min(preShotRemaining, outcome.distanceRemaining() + SimConstants.WATER_DROP_SETBACK);
                lie = Surface.PRIMARY_ROUGH; // dropped in rough near the hazard
            } else {
                remaining = preShotRemaining; // out of bounds: stroke-and-distance
            }
            holed = false;
        } else {
            remaining = outcome.distanceRemaining();
            holed = remaining <= SimConstants.HOLED_THRESHOLD;
        }
        shotNumber++;
        if (holed || shotNumber > SimConstants.MAX_SHOTS_PER_HOLE) {
            completeHole();
        }
        return outcome;
    }

    private void completeHole() {
        holeScores.add(strokesThisHole);
        int par = holes.get(holeIndex).par();
        shotStats = shotStats.plus(HoleStats.of(currentHoleShots, par));
        playedHoles.add(new PlayedHole(holeIndex + 1, par, currentHoleShots));
        currentHoleShots.clear();
        holeIndex++;
        if (!isComplete()) {
            remaining = holes.get(holeIndex).model().startDistance();
            shotNumber = 1;
            strokesThisHole = 0;
            lie = Surface.TEE_BOX;
            ball = initialBall(holes.get(holeIndex));
        }
    }

    /** The round's accumulated shot-level statistics (spec: competitive-statistics). */
    public ShotStatLine shotStats() {
        return shotStats;
    }

    /** Total strokes taken so far (the round total once complete). */
    public int totalStrokes() {
        return totalStrokes;
    }

    /** The score on each completed hole, in order. */
    public List<Integer> holeScores() {
        return List.copyOf(holeScores);
    }

    /** The completed holes with their full shot-by-shot detail, in order (spec: career-achievements). */
    public List<PlayedHole> playedHoles() {
        return List.copyOf(playedHoles);
    }

    /** The completed holes of this round as (hole number, par, strokes), in order. */
    public List<RoundScorecard.HoleScore> completedHoles() {
        List<RoundScorecard.HoleScore> result = new ArrayList<>(holeScores.size());
        for (int i = 0; i < holeScores.size(); i++) {
            result.add(new RoundScorecard.HoleScore(i + 1, holes.get(i).par(), holeScores.get(i)));
        }
        return result;
    }

    /** Strokes relative to par over the completed holes (the round's score vs par once complete). */
    public int scoreVsPar() {
        int strokes = 0;
        int par = 0;
        for (int i = 0; i < holeScores.size(); i++) {
            strokes += holeScores.get(i);
            par += holes.get(i).par();
        }
        return strokes - par;
    }

    private void requireNotComplete() {
        if (isComplete()) {
            throw new IllegalStateException("the round is complete");
        }
    }

    /** Current authoritative state for API projection; null only for a legacy fixture-backed round. */
    public BallState ballState() {
        return ball;
    }

    private ShotContext contextFor(HoleToPlay hole, ShotDecision decision, SeedCoordinate coord, Position2d humanAimTarget) {
        if (ball == null || hole.model().geometry() == null) {
            return new ShotContext(attributes, state, hole.environment(), remaining, hole.model().zoneProfileFor(remaining),
                    decision, coord, lie, hole.model().pinLateral());
        }
        ShotAim.Reference aim = humanAimTarget == null
                ? ShotAim.forBall(hole.model(), ball, decision.strategy())
                : new ShotAim.Reference(humanAimTarget, 0.0, false);
        return new ShotContext(attributes, state, hole.environment(), remaining, hole.model().zoneProfileFor(remaining),
                decision, coord, lie, aim.pinLateral(), ball, hole.model().geometry(), hole.model().cupPosition(), aim.target(), handedness);
    }

    private static void validateAim(Position2d target, com.progolf.sim.course.CourseGeometry geometry) {
        AimEnvelope envelope = envelope(geometry);
        if (!envelope.contains(new AimPoint(target.x(), target.y()))) {
            throw new IllegalArgumentException("aim point is outside the planning envelope");
        }
    }

    private String revision() { return (holeIndex + 1) + ":" + shotNumber; }

    private static AimEnvelope envelope(com.progolf.sim.course.CourseGeometry geometry) {
        return new AimEnvelope(
                geometry.playableBoundary().stream().mapToDouble(Position2d::x).min().orElseThrow() - 100.0,
                geometry.playableBoundary().stream().mapToDouble(Position2d::x).max().orElseThrow() + 100.0,
                geometry.playableBoundary().stream().mapToDouble(Position2d::y).min().orElseThrow() - 100.0,
                geometry.playableBoundary().stream().mapToDouble(Position2d::y).max().orElseThrow() + 100.0);
    }

    private ShotGuidance guidance(HoleModel model, com.progolf.sim.shot.Environment environment) {
        return new ShotGuidance(pointFor(model, Strategy.CONSERVATIVE), pointFor(model, Strategy.BALANCED),
                pointFor(model, Strategy.AGGRESSIVE), ClubSpec.all().stream().map(spec -> new ShotGuidance.ClubReach(
                spec.id(), spec.label(), spec.baseCarry(), spec.baseCarry() * (SimConstants.REACH_FLOOR
                        + SimConstants.REACH_SPAN * attributes.norm(spec.distanceAttribute())),
                java.util.Arrays.stream(com.progolf.sim.shot.ShotFamily.values()).map(family -> {
                    ShotFamilyEligibility.Result eligibility = ShotFamilyEligibility.evaluate(lie, spec, family);
                    return new ShotGuidance.FamilyAvailability(family, eligibility.allowed(), eligibility.reason(),
                            java.util.Arrays.stream(com.progolf.sim.shot.ShotShape.values()).map(shape -> {
                                var result = com.progolf.sim.shot.ShotShapeEligibility.evaluate(family, shape);
                                return new ShotGuidance.ShapeAvailability(shape, result.allowed(), result.reason());
                            }).toList());
                }).toList())).toList(), com.progolf.sim.shot.StrategicTargetPlanner.options(model, ball, remaining,
                lie, attributes, environment));
    }

    private AimPoint pointFor(HoleModel model, Strategy strategy) {
        Position2d point = ShotAim.forBall(model, ball, strategy).target();
        return new AimPoint(point.x(), point.y());
    }

    private static BallState initialBall(HoleToPlay hole) {
        return hole.model().geometry() == null ? null : new BallState(hole.model().geometry().tee(), Surface.TEE_BOX);
    }

    private record SimShot(ShotDecision decision, Position2d aimTarget) { }
}
