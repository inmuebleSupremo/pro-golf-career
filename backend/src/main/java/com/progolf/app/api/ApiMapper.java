package com.progolf.app.api;

import com.progolf.app.api.dto.CareerGoalDto;
import com.progolf.app.api.dto.EquipmentItemDto;
import com.progolf.app.api.dto.GolferDto;
import com.progolf.app.api.dto.HallOfFameDto;
import com.progolf.app.api.dto.LeaderboardRowDto;
import com.progolf.app.api.dto.SaveDto;
import com.progolf.app.api.dto.ScheduleEntryDto;
import com.progolf.app.api.dto.ShotSituationDto;
import com.progolf.app.api.dto.SponsorshipOfferDto;
import com.progolf.app.api.dto.StaffMemberDto;
import com.progolf.app.api.dto.WorldConfigInput;
import com.progolf.app.persistence.SaveMetadata;
import com.progolf.sim.career.HallOfFameInduction;
import com.progolf.sim.economy.SponsorshipAgreement;
import com.progolf.sim.economy.SponsorshipOffer;
import com.progolf.sim.equipment.EquipmentCharacteristics;
import com.progolf.sim.equipment.EquipmentItem;
import com.progolf.sim.play.ShotSituation;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.staff.StaffMember;
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
        return new ScheduleEntryDto(e.tournamentId(), e.week(), e.tier().name(), e.prestige().name(), e.entered());
    }

    public static CareerGoalDto careerGoal(CareerGoalProgress p) {
        return new CareerGoalDto(p.goal().type().name(), p.target(), p.current(), p.achieved());
    }

    public static HallOfFameDto hallOfFame(HallOfFameInduction i) {
        return new HallOfFameDto(i.golferId(), i.season(), i.score());
    }

    public static SponsorshipOfferDto sponsorship(SponsorshipOffer o) {
        SponsorshipAgreement a = o.agreement();
        return new SponsorshipOfferDto(a.sponsor(), a.perSeasonPayment(), a.signingBonus(),
                a.durationSeasons(), o.grossValue());
    }

    public static StaffMemberDto staff(StaffMember s) {
        return new StaffMemberDto(s.role().name(), s.name(), s.quality(), s.hiringCost(), s.seasonalSalary());
    }

    public static EquipmentItemDto equipment(EquipmentItem i) {
        EquipmentCharacteristics c = i.characteristics();
        return new EquipmentItemDto(i.name(), i.category().name(), i.quality(), i.cost(),
                c.forgiveness(), c.power(), c.workability(), c.feel());
    }

    public static LeaderboardRowDto leaderboardRow(LeaderboardEntry e) {
        return new LeaderboardRowDto(e.position(), golfer(e.golfer()), e.score(), e.roundsPlayed());
    }

    public static ShotSituationDto situation(ShotSituation s) {
        return new ShotSituationDto(s.holeNumber(), s.par(), s.shotNumber(), s.strokesThisHole(),
                s.distanceToPin(), s.lie().name(), s.pinLateral(),
                s.reachable().minReach(), s.reachable().maxReach());
    }

    public static SaveDto save(SaveMetadata m) {
        return new SaveDto(m.saveId(), m.savedAt().toString(), m.season(), m.week(), m.playerGolferId());
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
