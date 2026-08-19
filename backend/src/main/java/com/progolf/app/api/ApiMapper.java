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
import com.progolf.app.api.dto.ShotOutcomeDto;
import com.progolf.app.api.dto.PlayingHoleDto;
import com.progolf.app.api.dto.ShotSituationDto;
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
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.course.GeneratedHole;
import com.progolf.sim.course.PinPosition;
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

    private ApiMapper() {
    }

    /** A golfer's identity for a leaderboard/reference row: stable id + full name. */
    public static GolferDto golfer(ProfessionalGolfer g) {
        return new GolferDto(g.id(), g.player().identity().fullName());
    }

    public static ScheduleEntryDto schedule(PlayerScheduleEntry e) {
        return new ScheduleEntryDto(e.tournamentId(), e.week(), e.tier().name(), e.prestige().name(), e.entered(),
                e.name(), e.location(), e.courseType());
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
                s.reachable().minReach(), s.reachable().maxReach(), reachableBands(s.reachable()));
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

    /** Projects a hole's geometry and the active round's pin to the client rendering DTO (spec: web-hole-visualization). */
    public static PlayingHoleDto playingHole(GeneratedHole hole, PinPosition pin, EnvironmentClassification classification) {
        return new PlayingHoleDto(hole.number(), hole.par(), hole.length(),
                hole.fairwayHalfWidth(), hole.greenHalfWidth(), hole.greenDepth(), hole.elevationDelta(),
                hole.hasGreensideBunker(), hole.hasWater(), hole.hasTrees(),
                pin.lateralOffset(), pin.depthOffset(),
                classification.name(), Long.toString(hole.holeSeed()));
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
                o.hazardEntered(), o.penaltyStrokes(), o.strokes());
    }

    // --- Input parsing (enum-valued arguments arrive as their names; a bad name throws
    //     IllegalArgumentException, classified BAD_REQUEST by GraphQlErrorResolver) ---

    public static ShotDecision shotDecision(ShotDecisionInput in) {
        double lateral = in.targetLateral() != null ? in.targetLateral() : 0.0;
        return new ShotDecision(club(in.club()), in.targetDistance(), lateral, strategy(in.strategy()));
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
