package com.progolf.app.api;

import com.progolf.app.api.dto.AttributeRaiseInput;
import com.progolf.app.api.dto.AchievementDto;
import com.progolf.app.api.dto.CareerGoalDto;
import com.progolf.app.api.dto.CareerGoalInput;
import com.progolf.app.api.dto.EquipmentItemDto;
import com.progolf.app.api.dto.GolferDto;
import com.progolf.app.api.dto.LeaderboardRowDto;
import com.progolf.app.api.dto.NewsItemDto;
import com.progolf.app.api.dto.RoundScorecardDto;
import com.progolf.app.api.dto.SaveDto;
import com.progolf.app.api.dto.ScheduleEntryDto;
import com.progolf.app.api.dto.SeasonStatDto;
import com.progolf.app.api.dto.ShotDecisionInput;
import com.progolf.app.api.dto.BallStrikeIntentInput;
import com.progolf.app.api.dto.ShotFamilyAvailabilityDto;
import com.progolf.app.api.dto.ShotShapeAvailabilityDto;
import com.progolf.app.api.dto.AirbornePointDto;
import com.progolf.app.api.dto.ShotTraceRollDto;
import com.progolf.app.api.dto.AimEnvelopeDto;
import com.progolf.app.api.dto.AimPointDto;
import com.progolf.app.api.dto.ClubReachDto;
import com.progolf.app.api.dto.ShotGuidanceDto;
import com.progolf.app.api.dto.StrategicTargetOptionDto;
import com.progolf.app.api.dto.ShotSubmissionDto;
import com.progolf.app.api.dto.ShotOutcomeDto;
import com.progolf.app.api.dto.PlayingHoleDto;
import com.progolf.app.api.dto.EffectiveWindDto;
import com.progolf.app.api.dto.PlayingGeometryDto;
import com.progolf.app.api.dto.PositionDto;
import com.progolf.app.api.dto.TerrainRegionDto;
import com.progolf.app.api.dto.BallStateDto;
import com.progolf.app.api.dto.ShotContactDto;
import com.progolf.app.api.dto.ShotSettlementDto;
import com.progolf.app.api.dto.ShotTraceDto;
import com.progolf.app.api.dto.ShotTraceTransitionDto;
import com.progolf.app.api.dto.ShotSituationDto;
import com.progolf.app.api.dto.PhysicalGreenDto;
import com.progolf.app.api.dto.GreenSlopeSampleDto;
import com.progolf.app.api.dto.GreenContourDto;
import com.progolf.app.api.dto.PhysicalPuttOutcomeDto;
import com.progolf.app.api.dto.PhysicalPuttTrajectoryPointDto;
import com.progolf.app.api.dto.PhysicalPuttIntentInput;
import com.progolf.app.api.dto.SurfaceBandDto;
import com.progolf.app.api.dto.SurfaceRegionDto;
import com.progolf.app.api.dto.SponsorshipOfferDto;
import com.progolf.app.api.dto.StaffMemberDto;
import com.progolf.app.api.dto.WorldConfigInput;
import com.progolf.app.persistence.SaveMetadata;
import com.progolf.sim.achievement.Achievement;
import com.progolf.sim.control.CareerGoal;
import com.progolf.sim.control.GoalType;
import com.progolf.sim.core.Attribute;
import com.progolf.sim.economy.SponsorshipAgreement;
import com.progolf.sim.economy.SponsorshipOffer;
import com.progolf.sim.equipment.EquipmentCharacteristics;
import com.progolf.sim.equipment.EquipmentItem;
import com.progolf.sim.media.NewsEvent;
import com.progolf.sim.course.GeneratedHole;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.PinPosition;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.GreenSurface;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.putt.PhysicalPuttIntent;
import com.progolf.sim.shot.WindVector;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.ShotSettlement;
import com.progolf.sim.shot.ShotTrace;
import com.progolf.sim.shot.ShotTraceTransition;
import com.progolf.sim.play.RoundScorecard;
import com.progolf.sim.play.ShotSituation;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.shot.Club;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.ShotFamily;
import com.progolf.sim.shot.ShotShape;
import com.progolf.sim.staff.StaffMember;
import com.progolf.sim.staff.StaffRole;
import com.progolf.sim.statistics.SeasonStatistics;
import com.progolf.sim.statistics.StatLine;
import com.progolf.sim.tournament.LeaderboardEntry;
import com.progolf.sim.world.CareerGoalProgress;
import com.progolf.sim.world.PlayerScheduleEntry;
import com.progolf.sim.world.WorldConfig;
import java.util.List;

/**
 * The single API-edge projection (capability graphql-api, design D2): maps simulation-engine read types into
 * application-layer DTOs so no {@code sim.*} record ever appears in the GraphQL schema or a resolver
 * signature. Pure and static; enum-valued engine fields are surfaced as their {@code name()}.
 */
public final class ApiMapper {
    /** Enough solver-owned gradient samples for a close putting viewport without changing GreenSurface itself. */
    private static final int GREEN_READING_SAMPLE_DIVISIONS = 16;
    private static final int GREEN_READING_CONTOUR_LEVELS = 9;

    private ApiMapper() {
    }

    /** A golfer's identity for a leaderboard/reference row: stable id + full name. */
    public static GolferDto golfer(ProfessionalGolfer g) {
        return new GolferDto(g.id(), g.player().identity().fullName());
    }

    public static ScheduleEntryDto schedule(PlayerScheduleEntry e) {
        return new ScheduleEntryDto(e.tournamentId(), e.week(), e.tier().name(), e.prestige().name(), e.entered(),
                e.name(), e.location(), e.courseType(), e.puttingPolicy().name());
    }

    public static CareerGoalDto careerGoal(CareerGoalProgress p) {
        return new CareerGoalDto(p.goal().type().name(), p.target(), p.current(), p.achieved());
    }

    /** Projects one catalogue achievement + its unlock season ({@code null} = locked) to its GraphQL DTO. */
    public static AchievementDto achievement(Achievement a, Integer seasonUnlocked) {
        boolean unlocked = seasonUnlocked != null;
        // A secret achievement withholds its description until it is earned.
        String description = (a.secret() && !unlocked) ? null : a.description();
        return new AchievementDto(a.name(), a.category().name(), a.category().label(), a.title(),
                description, a.secret(), unlocked, seasonUnlocked);
    }

    public static SponsorshipOfferDto sponsorship(SponsorshipOffer o) {
        SponsorshipAgreement a = o.agreement();
        return new SponsorshipOfferDto(a.sponsor(), a.industry(), a.perSeasonPayment(), a.signingBonus(),
                a.durationSeasons(), o.grossValue());
    }

    public static StaffMemberDto staff(StaffMember s) {
        return new StaffMemberDto(s.role().name(), s.name(), s.age(), s.nationality(),
                s.personality().name(), s.quality(), s.hiringCost(), s.seasonalSalary());
    }

    public static LeaderboardRowDto leaderboardRow(LeaderboardEntry e) {
        return new LeaderboardRowDto(e.position(), golfer(e.golfer()), e.score(), e.roundsPlayed());
    }

    public static ShotSituationDto situation(ShotSituation s) {
        return new ShotSituationDto(s.holeNumber(), s.par(), s.shotNumber(), s.strokesThisHole(),
                s.distanceToPin(), s.lie().name(), s.pinLateral(),
                s.reachable().minReach(), s.reachable().maxReach(), reachableBands(s.reachable()), s.shotRevision(),
                s.aimEnvelope() == null ? null : new AimEnvelopeDto(s.aimEnvelope().minX(), s.aimEnvelope().maxX(),
                        s.aimEnvelope().minY(), s.aimEnvelope().maxY()), guidance(s));
    }

    private static ShotGuidanceDto guidance(ShotSituation situation) {
        if (situation.guidance() == null) return null;
        var g = situation.guidance();
        return new ShotGuidanceDto(point(g.safe()), point(g.primary()), point(g.aggressive()), g.clubs().stream()
                .map(c -> new ClubReachDto(c.club().name(), c.label(), c.nominalCarry(), c.normalReach(), c.families().stream()
                        .map(f -> new ShotFamilyAvailabilityDto(f.family().name(), f.available(), f.reason(), f.shapes().stream()
                                .map(s -> new ShotShapeAvailabilityDto(s.shape().name(), s.available(), s.reason())).toList())).toList())).toList(),
                g.strategicOptions().stream().map(option -> new StrategicTargetOptionDto(option.role().name(),
                        point(option.aimPoint()), option.suggestedClub().name(), option.suggestedFamily().name(),
                        option.routeSummary(), option.exposureSummary())).toList());
    }

    public static ShotSubmissionDto submission(com.progolf.sim.play.ShotSubmission submission) {
        return new ShotSubmissionDto(submission.outcome() == null ? null : shotOutcome(submission.outcome()), submission.stale());
    }

    /** Projects the reachable zone profile to ordered surface bands for a truthful shot-preview overlay. */
    private static List<SurfaceBandDto> reachableBands(ShotZoneProfile profile) {
        return profile.bands().stream()
                .map(b -> new SurfaceBandDto(b.startDistance(), b.endDistance(),
                        b.regions().stream()
                                .map(r -> new SurfaceRegionDto(r.surface().name(), r.outerHalfWidth()))
                                .toList()))
                .toList();
    }

    /**
     * Projects a hole's geometry and the active round's pin to the client rendering DTO (spec:
     * web-hole-visualization). {@code courseType} is the event's canonical scene token (the same token the scene
     * backdrop uses), so the hole's biome illustration always agrees with the event's name, place, and photo.
     */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType, BallState ball) {
        return playingHole(hole, pin, courseType, courseType, CourseGenConstants.V1_GENERATOR_VERSION, ball, hole.geometry());
    }

    /** Projects the effective setup-specific geometry used by the active playable hole. */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType, String canonicalBiome,
                                              int courseGeneratorVersion, BallState ball, CourseGeometry geometry) {
        return playingHole(hole, pin, courseType, canonicalBiome, courseGeneratorVersion, ball, geometry,
                legacyCup(geometry, pin), new WindVector(0.0, 0.0));
    }

    /** Compatibility overload for callers that do not carry a retained generator identity. */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType, BallState ball,
                                              CourseGeometry geometry) {
        return playingHole(hole, pin, courseType, courseType, CourseGenConstants.V1_GENERATOR_VERSION, ball, geometry);
    }

    /** Projects the effective setup-specific geometry and resolver-effective canonical wind for the active hole. */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType, String canonicalBiome,
                                              int courseGeneratorVersion, BallState ball, CourseGeometry geometry, WindVector wind) {
        return playingHole(hole, pin, courseType, canonicalBiome, courseGeneratorVersion, ball, geometry,
                legacyCup(geometry, pin), wind);
    }

    /** Compatibility overload for callers that do not carry a retained generator identity. */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType, BallState ball,
                                              CourseGeometry geometry, WindVector wind) {
        return playingHole(hole, pin, courseType, courseType, CourseGenConstants.V1_GENERATOR_VERSION, ball, geometry, wind);
    }

    /** Projects a policy-resolved cup rather than reconstructing it from legacy offsets. */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType, String canonicalBiome,
                                              int courseGeneratorVersion, BallState ball, CourseGeometry geometry, Position2d cup) {
        return playingHole(hole, pin, courseType, canonicalBiome, courseGeneratorVersion, ball, geometry, cup,
                new WindVector(0.0, 0.0));
    }

    /** Compatibility overload for callers that do not carry a retained generator identity. */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType, BallState ball,
                                              CourseGeometry geometry, Position2d cup) {
        return playingHole(hole, pin, courseType, courseType, CourseGenConstants.V1_GENERATOR_VERSION, ball, geometry, cup);
    }

    /** Projects the policy-resolved cup and resolver-effective canonical wind used by the active hole. */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType, String canonicalBiome,
                                              int courseGeneratorVersion, BallState ball, CourseGeometry geometry,
                                              Position2d cup, WindVector wind) {
        return playingHole(hole, pin, courseType, canonicalBiome, courseGeneratorVersion, ball, geometry, cup, wind,
                PuttingPolicy.LEGACY, null);
    }

    /** Projects a policy-bearing playing hole and a sparse, solver-authored green reading when physical. */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType, String canonicalBiome,
                                               int courseGeneratorVersion, BallState ball, CourseGeometry geometry,
                                               Position2d cup, WindVector wind, PuttingPolicy puttingPolicy,
                                               GreenSurface greenSurface) {
        return new PlayingHoleDto(hole.number(), hole.par(), hole.length(),
                hole.fairwayHalfWidth(), hole.greenHalfWidth(), hole.greenDepth(), hole.elevationDelta(),
                hole.hasGreensideBunker(), hole.hasWater(), hole.hasTrees(),
                pin.lateralOffset(), pin.depthOffset(),
                courseType, canonicalBiome, courseGeneratorVersion, geometrySource(courseGeneratorVersion),
                Long.toString(hole.holeSeed()), geometry(geometry, cup), ballState(ball), effectiveWind(wind),
                puttingPolicy.name(), physicalGreen(greenSurface));
    }

    /** Compatibility mapper for tests/readers not yet carrying a live ball. */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, String courseType) {
        return playingHole(hole, pin, courseType, new BallState(hole.geometry().tee(), com.progolf.sim.spatial.Surface.TEE_BOX));
    }

    private static String geometrySource(int version) {
        return switch (version) {
            case CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION -> "GENERATOR_8_PRINCIPLED_ARCHITECTURE";
            case CourseGenConstants.V5_REFINED_GENERATOR_VERSION -> "GENERATOR_7_REFINED_V5";
            default -> "GENERATOR_" + version + "_CANONICAL";
        };
    }

    public static SeasonStatDto seasonStat(SeasonStatistics s) {
        StatLine l = s.line();
        return new SeasonStatDto(s.season(), l.events(), l.wins(), l.topTens(), l.cuts(), l.bestFinish(),
                l.earnings());
    }

    public static NewsItemDto news(NewsEvent e) {
        return new NewsItemDto(e.season(), e.type().name(), e.headline(), e.prominence(),
                e.subjectGolferId().orElse(null));
    }

    public static RoundScorecardDto scorecard(RoundScorecard s) {
        List<RoundScorecardDto.HoleScoreDto> holes = s.holes().stream()
                .map(h -> new RoundScorecardDto.HoleScoreDto(h.holeNumber(), h.par(), h.strokes()))
                .toList();
        return new RoundScorecardDto(s.roundNumber(), s.currentHole(), s.scoreToPar(), s.totalStrokes(), holes);
    }

    public static SaveDto save(SaveMetadata m) {
        return new SaveDto(m.saveId(), m.savedAt().toString(), m.season(), m.week(), m.playerGolferId());
    }

    public static ShotOutcomeDto shotOutcome(ShotOutcome o) {
        return new ShotOutcomeDto(o.finalSurface().name(), o.carry(), o.lateral(), o.distanceRemaining(),
                o.holed(), o.hazardEntered(), o.penaltyStrokes(), o.strokes(), settlement(o.settlement()), trace(o.trace()),
                physicalPutt(o.physicalPutt()));
    }

    private static PhysicalPuttOutcomeDto physicalPutt(com.progolf.sim.putt.PhysicalPuttOutcome outcome) {
        return outcome == null ? null : new PhysicalPuttOutcomeDto(outcome.trajectory().stream()
                .map(point -> new PhysicalPuttTrajectoryPointDto(point.timeSeconds(), point(point.position()),
                        point.speedYardsPerSecond())).toList(), point(outcome.finalPosition()), outcome.holed(),
                outcome.terminalCondition().name());
    }

    /** Samples the existing immutable physical field only; no UI-side terrain model is created. */
    private static PhysicalGreenDto physicalGreen(GreenSurface surface) {
        if (surface == null) return null;
        int columnStep = Math.max(1, (surface.columns() - 1) / GREEN_READING_SAMPLE_DIVISIONS);
        int rowStep = Math.max(1, (surface.rows() - 1) / GREEN_READING_SAMPLE_DIVISIONS);
        List<GreenSlopeSampleDto> samples = new java.util.ArrayList<>();
        for (int row = 0; row < surface.rows(); row += rowStep) for (int column = 0; column < surface.columns(); column += columnStep) {
            Position2d position = surface.gridPoint(column, row);
            if (!surface.contains(position)) continue;
            var sample = surface.sampleAt(position);
            Position2d downhill = sample.gradient().downhillDirection();
            samples.add(new GreenSlopeSampleDto(point(position), sample.elevation(), downhill.x(), downhill.y(),
                    sample.gradient().magnitude()));
        }
        return new PhysicalGreenDto(List.copyOf(samples), contours(surface));
    }

    /** Marching-squares contours from the solver-owned grid; the app neither smooths nor invents terrain. */
    private static List<GreenContourDto> contours(GreenSurface surface) {
        double range = surface.maximumElevation() - surface.minimumElevation();
        if (range < 1.0e-9) return List.of();
        List<GreenContourDto> result = new java.util.ArrayList<>();
        for (int levelIndex = 1; levelIndex <= GREEN_READING_CONTOUR_LEVELS; levelIndex++) {
            double elevation = surface.minimumElevation() + range * levelIndex / (GREEN_READING_CONTOUR_LEVELS + 1.0);
            List<ContourSegment> segments = new java.util.ArrayList<>();
            for (int row = 0; row < surface.rows() - 1; row++) for (int column = 0; column < surface.columns() - 1; column++) {
                Position2d[] points = {surface.gridPoint(column, row), surface.gridPoint(column + 1, row),
                        surface.gridPoint(column + 1, row + 1), surface.gridPoint(column, row + 1)};
                double[] heights = {surface.gridElevation(column, row), surface.gridElevation(column + 1, row),
                        surface.gridElevation(column + 1, row + 1), surface.gridElevation(column, row + 1)};
                List<Position2d> crossings = new java.util.ArrayList<>();
                contourCrossing(crossings, points[0], heights[0], points[1], heights[1], elevation);
                contourCrossing(crossings, points[1], heights[1], points[2], heights[2], elevation);
                contourCrossing(crossings, points[2], heights[2], points[3], heights[3], elevation);
                contourCrossing(crossings, points[3], heights[3], points[0], heights[0], elevation);
                if (crossings.size() == 2) addContourSegment(segments, surface, crossings.get(0), crossings.get(1));
                if (crossings.size() == 4) {
                    addContourSegment(segments, surface, crossings.get(0), crossings.get(1));
                    addContourSegment(segments, surface, crossings.get(2), crossings.get(3));
                }
            }
            for (List<Position2d> line : stitchContours(segments)) {
                if (line.size() >= 2) result.add(new GreenContourDto(elevation, line.stream().map(ApiMapper::point).toList()));
            }
        }
        return List.copyOf(result);
    }

    private static void contourCrossing(List<Position2d> crossings, Position2d a, double heightA, Position2d b,
                                        double heightB, double elevation) {
        if ((heightA < elevation && heightB >= elevation) || (heightB < elevation && heightA >= elevation)) {
            double fraction = (elevation - heightA) / (heightB - heightA);
            crossings.add(new Position2d(a.x() + (b.x() - a.x()) * fraction, a.y() + (b.y() - a.y()) * fraction));
        }
    }

    private static void addContourSegment(List<ContourSegment> segments, GreenSurface surface, Position2d a, Position2d b) {
        if (surface.contains(a) && surface.contains(b)) segments.add(new ContourSegment(a, b));
    }

    private static List<List<Position2d>> stitchContours(List<ContourSegment> segments) {
        List<ContourSegment> remaining = new java.util.ArrayList<>(segments);
        List<List<Position2d>> lines = new java.util.ArrayList<>();
        while (!remaining.isEmpty()) {
            ContourSegment first = remaining.remove(remaining.size() - 1);
            List<Position2d> line = new java.util.ArrayList<>(List.of(first.a(), first.b()));
            boolean joined = true;
            while (joined) {
                joined = false;
                for (int index = 0; index < remaining.size(); index++) {
                    ContourSegment candidate = remaining.get(index);
                    if (samePoint(line.get(line.size() - 1), candidate.a())) { line.add(candidate.b()); }
                    else if (samePoint(line.get(line.size() - 1), candidate.b())) { line.add(candidate.a()); }
                    else if (samePoint(line.get(0), candidate.b())) { line.add(0, candidate.a()); }
                    else if (samePoint(line.get(0), candidate.a())) { line.add(0, candidate.b()); }
                    else continue;
                    remaining.remove(index);
                    joined = true;
                    break;
                }
            }
            lines.add(List.copyOf(line));
        }
        return lines;
    }

    private static boolean samePoint(Position2d a, Position2d b) {
        return a.distanceTo(b) < 1.0e-7;
    }

    private record ContourSegment(Position2d a, Position2d b) { }

    private static PlayingGeometryDto geometry(CourseGeometry geometry, Position2d cup) {
        return new PlayingGeometryDto(point(geometry.tee()), point(cup),
                geometry.playableBoundary().stream().map(ApiMapper::point).toList(),
                geometry.regions().stream().map(region -> new TerrainRegionDto(region.surface().name(),
                        region.boundary().stream().map(ApiMapper::point).toList())).toList());
    }

    private static Position2d legacyCup(CourseGeometry geometry, PinPosition pin) {
        return new Position2d(geometry.greenCenter().x() + pin.lateralOffset(),
                geometry.greenCenter().y() + pin.depthOffset());
    }

    private static PositionDto point(Position2d point) {
        return new PositionDto(point.x(), point.y());
    }

    private static AimPointDto point(AimPoint point) {
        return new AimPointDto(point.x(), point.y());
    }

    private static BallStateDto ballState(BallState ball) {
        return ball == null ? null : new BallStateDto(point(ball.position()), ball.lie().name());
    }

    private static EffectiveWindDto effectiveWind(WindVector wind) {
        return new EffectiveWindDto(wind.x(), wind.y(), StrictMath.hypot(wind.x(), wind.y()), "EFFECTIVE_YARDS");
    }

    private static ShotSettlementDto settlement(ShotSettlement settlement) {
        return settlement == null ? null : new ShotSettlementDto(
                new ShotContactDto(point(settlement.contact().position()), settlement.contact().surface().name()),
                settlement.recoveryPosition() == null ? null : point(settlement.recoveryPosition()),
                settlement.recoveryKind().name(), ballState(settlement.ball()));
    }

    private static ShotTraceDto trace(ShotTrace trace) {
        return trace == null ? null : new ShotTraceDto(trace.clubId().name(), point(trace.origin()),
                point(trace.intendedAimPoint()), new ShotContactDto(point(trace.contact().position()),
                trace.contact().surface().name()), trace.airbornePath().stream()
                .map(p -> new AirbornePointDto(p.progress(), point(p.position()), p.height())).toList(), roll(trace.roll()), transition(trace.transition()), point(trace.finalPoint()));
    }

    private static ShotTraceRollDto roll(com.progolf.sim.shot.ShotTraceRoll roll) {
        return roll == null ? null : new ShotTraceRollDto(point(roll.from()), point(roll.to()));
    }

    private static ShotTraceTransitionDto transition(ShotTraceTransition transition) {
        return transition == null ? null : new ShotTraceTransitionDto(transition.kind().name(), point(transition.from()),
                point(transition.to()));
    }

    // --- Input parsing (enum-valued arguments arrive as their names; a bad name throws
    //     IllegalArgumentException, classified BAD_REQUEST by GraphQlErrorResolver) ---

    public static ShotDecision shotDecision(ShotDecisionInput in) {
        double lateral = in.targetLateral() != null ? in.targetLateral() : 0.0;
        return new ShotDecision(club(in.club()), in.targetDistance(), lateral, strategy(in.strategy()));
    }

    public static BallStrikeIntent ballStrikeIntent(BallStrikeIntentInput in) {
        if (in == null || in.aimPoint() == null) throw new IllegalArgumentException("aimPoint is required");
        ShotFamily family = in.shotFamily() == null ? ShotFamily.FULL : ShotFamily.valueOf(in.shotFamily());
        ShotShape shape = in.shotShape() == null ? ShotShape.STRAIGHT : ShotShape.valueOf(in.shotShape());
        return new BallStrikeIntent(ClubId.valueOf(in.club()), new AimPoint(in.aimPoint().x(), in.aimPoint().y()), family, shape);
    }

    public static PhysicalPuttIntent physicalPuttIntent(PhysicalPuttIntentInput in) {
        if (in == null || in.direction() == null) throw new IllegalArgumentException("physical putt direction is required");
        return new PhysicalPuttIntent(new Position2d(in.direction().x(), in.direction().y()), in.intendedRollFeet());
    }

    public static CareerGoal careerGoal(CareerGoalInput in) {
        GoalType type = goalType(in.type());
        return in.target() != null ? CareerGoal.of(type, in.target()) : CareerGoal.of(type);
    }

    public static List<CareerGoal> careerGoals(List<CareerGoalInput> in) {
        return in.stream().map(ApiMapper::careerGoal).toList();
    }

    public static List<Attribute> attributes(List<String> names) {
        return names.stream().map(ApiMapper::attribute).toList();
    }

    /** Parses spend inputs into an attribute→levels map, summing duplicates and dropping non-positive raises. */
    public static java.util.Map<Attribute, Integer> raises(List<AttributeRaiseInput> inputs) {
        java.util.Map<Attribute, Integer> map = new java.util.EnumMap<>(Attribute.class);
        for (AttributeRaiseInput in : inputs) {
            if (in.points() > 0) {
                map.merge(attribute(in.attribute()), in.points(), Integer::sum);
            }
        }
        return map;
    }

    public static Club club(String name) {
        return Club.valueOf(name);
    }

    public static Strategy strategy(String name) {
        return Strategy.valueOf(name);
    }

    public static Attribute attribute(String name) {
        return Attribute.valueOf(name);
    }

    public static Nationality nationality(String name) {
        return Nationality.valueOf(name);
    }

    public static Archetype archetype(String name) {
        return Archetype.valueOf(name);
    }

    public static StaffRole staffRole(String name) {
        return StaffRole.valueOf(name);
    }

    public static com.progolf.sim.equipment.EquipmentCategory equipmentCategory(String name) {
        return com.progolf.sim.equipment.EquipmentCategory.valueOf(name);
    }

    public static GoalType goalType(String name) {
        return GoalType.valueOf(name);
    }

    /**
     * Maps a nullable GraphQL config input to a {@link WorldConfig}, filling any unset field from
     * {@link WorldConfig#defaults()}. Returns null when the whole input is null (the caller then uses the
     * default-config {@code create(seed)} overload).
     */
    public static WorldConfig worldConfig(WorldConfigInput in) {
        if (in == null) {
            return null;
        }
        WorldConfig d = WorldConfig.defaults();
        return new WorldConfig(
                in.populationSize() != null ? in.populationSize() : d.populationSize(),
                in.weeksPerSeason() != null ? in.weeksPerSeason() : d.weeksPerSeason(),
                in.eventsPerTierPerSeason() != null ? in.eventsPerTierPerSeason() : d.eventsPerTierPerSeason(),
                in.fieldSize() != null ? in.fieldSize() : d.fieldSize(),
                in.coursePoolSize() != null ? in.coursePoolSize() : d.coursePoolSize(),
                in.majorsPerSeason() != null ? in.majorsPerSeason() : d.majorsPerSeason(),
                in.signatureEventsPerTier() != null ? in.signatureEventsPerTier() : d.signatureEventsPerTier());
    }

    static <T, R> List<R> mapList(List<T> src, java.util.function.Function<T, R> f) {
        return src.stream().map(f).toList();
    }
}
