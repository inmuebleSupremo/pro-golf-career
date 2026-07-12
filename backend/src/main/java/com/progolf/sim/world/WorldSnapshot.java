package com.progolf.sim.world;

import com.progolf.sim.career.Career;
import com.progolf.sim.economy.FinancialAccount;
import com.progolf.sim.equipment.EquipmentInventory;
import com.progolf.sim.equipment.TournamentLoadout;
import com.progolf.sim.career.HallOfFameInduction;
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
        Set<String> announcedProspects) {

    /** An archived season captured by id (its schedule is immutable; its results re-link on restore). */
    public record ArchiveSnapshot(int season, List<ScheduledTournament> schedule,
                                  List<TournamentResult.Snapshot> results) {
    }
}
