package com.progolf.sim.world;

import com.progolf.sim.achievement.Achievement;
import com.progolf.sim.career.Career;
import com.progolf.sim.career.CareerRecordBook;
import com.progolf.sim.career.HallOfFameInduction;
import com.progolf.sim.control.CareerGoal;
import com.progolf.sim.control.PlayerControl;
import com.progolf.sim.economy.FinancialAccount;
import com.progolf.sim.economy.SponsorshipOffer;
import com.progolf.sim.equipment.EquipmentDeal;
import com.progolf.sim.equipment.EquipmentInventory;
import com.progolf.sim.equipment.EquipmentItem;
import com.progolf.sim.equipment.TournamentLoadout;
import com.progolf.sim.staff.StaffMember;
import com.progolf.sim.health.HealthEvent;
import com.progolf.sim.health.PhysicalState;
import com.progolf.sim.media.MediaSystem;
import com.progolf.sim.weather.EnvironmentalRecord;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.ranking.RankingSnapshot;
import com.progolf.sim.ranking.WorldRanking;
import com.progolf.sim.staff.SupportTeam;
import com.progolf.sim.statistics.StatisticsArchive;
import com.progolf.sim.tour.TourSystem;
import com.progolf.sim.tournament.TournamentResult;
import com.progolf.sim.course.PinPlacementVersion;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A true, immutable state capture of an autonomous {@link World} (spec: world-snapshot). Everything that a
 * running world accumulates is here; the seed-derived parts (course pool, weather system, stateless markets)
 * are NOT stored — they are regenerated from {@code (masterSeed, config)} on {@link World#restore}. This
 * slice captures an autonomous world; player-control state is the next slice.
 *
 * <p>Records that reference golfers are captured by golfer id ({@link ResultSnapshot}) and re-linked to the
 * rebuilt registry on restore; every other field is either a primitive, an immutable record, or a per-domain
 * snapshot record.
 */
public record WorldSnapshot(
        int season,
        int week,
        long nextTournamentId,
        int replenishCounter,
        String previousNumberOne,
        List<ProfessionalGolfer.Snapshot> golfers,
        Map<String, Career.Snapshot> careers,
        Set<String> activeGolfers,
        Map<String, FinancialAccount.Snapshot> accounts,
        Map<String, PhysicalState> physicalStates,
        Map<String, SupportTeam.Snapshot> supportTeams,
        Map<String, EquipmentInventory.Snapshot> equipment,
        Map<String, TournamentLoadout> loadouts,
        TourSystem.Snapshot tours,
        WorldRanking.Snapshot ranking,
        StatisticsArchive.Snapshot statistics,
        MediaSystem.Snapshot media,
        List<HallOfFameInduction> hallOfFameInductions,
        Set<String> hallOfFameMembers,
        Map<String, Integer> retirementSeason,
        List<ArchiveSnapshot> archives,
        List<RankingSnapshot> rankingSnapshots,
        List<EnvironmentalRecord> environmentalHistory,
        List<HealthEvent> healthHistory,
        List<TournamentResult.Snapshot> seasonResults,
        List<ScheduledTournament> schedule,
        Set<String> announcedProspects,
        PlayerControl.Snapshot playerControl,
        List<SponsorshipOffer> playerPendingOffers,
        List<StaffMember> playerPendingStaff,
        List<EquipmentItem> playerPendingEquipment,
        Set<CareerGoal> achievedGoals,
        List<StaffMember> staffPool,
        EquipmentDeal playerActiveEquipmentDeal,
        List<EquipmentDeal> playerPendingEquipmentDeals,
        Map<Achievement, Integer> unlockedAchievements,
        Set<Integer> majorsWonThisSeason,
        CareerRecordBook.Snapshot playerCareerRecords,
        Integer courseGeneratorVersion,
        PinPlacementVersion defaultPinPlacementVersion) {

    /** An archived season captured by id (its schedule is immutable; its results re-link on restore). */
    public record ArchiveSnapshot(int season, List<ScheduledTournament> schedule,
                                  List<TournamentResult.Snapshot> results) {
    }
}
