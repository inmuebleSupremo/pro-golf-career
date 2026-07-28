package com.progolf.app.api;

import com.progolf.app.api.dto.CareerGoalDto;
import com.progolf.app.api.dto.DevelopmentDeltaDto;
import com.progolf.app.api.dto.PlayerDevelopmentDto;
import com.progolf.app.api.dto.CurrentEventDto;
import com.progolf.app.api.dto.EquipmentDealDto;
import com.progolf.app.api.dto.EquipmentItemDto;
import com.progolf.app.api.dto.HallOfFameDto;
import com.progolf.app.api.dto.PlayerFitnessDto;
import com.progolf.app.api.dto.RankingRowDto;
import com.progolf.app.api.dto.RecordDto;
import com.progolf.app.api.dto.LeaderboardRowDto;
import com.progolf.app.api.dto.PlayerProfileDto;
import com.progolf.app.api.dto.SaveDto;
import com.progolf.app.api.dto.ScheduleEntryDto;
import com.progolf.app.api.dto.SeasonReviewDto;
import com.progolf.app.api.dto.NewsItemDto;
import com.progolf.app.api.dto.CalendarEntryDto;
import com.progolf.app.api.dto.RoundScorecardDto;
import com.progolf.app.api.dto.SeasonStatDto;
import com.progolf.app.api.dto.ShotSituationDto;
import com.progolf.app.api.dto.SponsorshipOfferDto;
import com.progolf.app.api.dto.SponsorshipStatusDto;
import com.progolf.app.api.dto.StaffMemberDto;
import com.progolf.app.api.dto.WorldStatusDto;
import com.progolf.app.auth.AuthenticatedUser;
import com.progolf.app.world.WorldService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * The GraphQL read model (capability graphql-api): {@code @QueryMapping} resolvers delegating to
 * {@link WorldService} and returning application DTOs (via {@link ApiMapper}) — no {@code sim.*} type is
 * exposed. Every operation is scoped to the authenticated user (spec: resource-ownership): the owner is read
 * from the security context, not from client input, so a query only ever sees the caller's own session.
 * Playable-event reads are safe off-event: they return null / an empty list when no player event is pending.
 */
@Controller
public class WorldQueryController {

    private final WorldService worldService;

    public WorldQueryController(WorldService worldService) {
        this.worldService = worldService;
    }

    @QueryMapping
    public WorldStatusDto world(@Argument String id) {
        return worldService.status(AuthenticatedUser.requireId(), id);
    }

    @QueryMapping
    public List<ScheduleEntryDto> playerSchedule(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        // Player-scoped: the engine requires a player; report empty rather than error when none is assigned.
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.playerSchedule(owner, id), ApiMapper::schedule);
    }

    @QueryMapping
    public List<CalendarEntryDto> playerCalendar(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return worldService.playerCalendar(owner, id);
    }

    @QueryMapping
    public List<CareerGoalDto> careerGoals(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.careerGoals(owner, id), ApiMapper::careerGoal);
    }

    @QueryMapping
    public PlayerProfileDto playerProfile(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return null;
        }
        return worldService.playerProfile(owner, id);
    }

    @QueryMapping
    public List<SeasonStatDto> playerSeasonStats(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.playerSeasonStats(owner, id), ApiMapper::seasonStat);
    }

    @QueryMapping
    public SeasonReviewDto seasonReview(@Argument String id, @Argument Integer season) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return null;
        }
        return worldService.seasonReview(owner, id, season);
    }

    @QueryMapping
    public List<String> playerDevelopmentFocus(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return worldService.playerDevelopmentFocus(owner, id);
    }

    @QueryMapping
    public List<DevelopmentDeltaDto> developmentReport(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return worldService.developmentReport(owner, id);
    }

    @QueryMapping
    public PlayerDevelopmentDto playerDevelopment(@Argument String id) {
        // Returns 0 points (with the cost constants) when no player is assigned — no guard needed.
        return worldService.playerDevelopment(AuthenticatedUser.requireId(), id);
    }

    @QueryMapping
    public List<HallOfFameDto> hallOfFame(@Argument String id) {
        // Name-enriched in WorldService (needs the world to resolve golfer names), so no ApiMapper step.
        return worldService.hallOfFame(AuthenticatedUser.requireId(), id);
    }

    @QueryMapping
    public List<RankingRowDto> worldRankings(@Argument String id, @Argument Integer limit) {
        // Name-enriched in WorldService; not player-scoped (the world ranking exists without a player).
        return worldService.worldRankings(AuthenticatedUser.requireId(), id, limit == null ? 100 : limit);
    }

    @QueryMapping
    public PlayerFitnessDto playerFitness(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return null;
        }
        return worldService.playerFitness(owner, id);
    }

    @QueryMapping
    public List<RecordDto> records(@Argument String id) {
        // Name-enriched in WorldService; not player-scoped (records exist without a player).
        return worldService.records(AuthenticatedUser.requireId(), id);
    }

    @QueryMapping
    public List<NewsItemDto> newsFeed(@Argument String id, @Argument Integer limit) {
        int n = limit == null ? 20 : limit;
        return ApiMapper.mapList(worldService.recentNews(AuthenticatedUser.requireId(), id, n), ApiMapper::news);
    }

    @QueryMapping
    public List<SponsorshipOfferDto> pendingSponsorships(@Argument String id) {
        return ApiMapper.mapList(worldService.pendingSponsorships(AuthenticatedUser.requireId(), id),
                ApiMapper::sponsorship);
    }

    @QueryMapping
    public SponsorshipStatusDto sponsorshipStatus(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return null;
        }
        return worldService.sponsorshipStatus(owner, id);
    }

    @QueryMapping
    public List<StaffMemberDto> pendingStaff(@Argument String id) {
        return ApiMapper.mapList(worldService.pendingStaffOffers(AuthenticatedUser.requireId(), id), ApiMapper::staff);
    }

    @QueryMapping
    public List<StaffMemberDto> playerStaff(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.playerStaff(owner, id), ApiMapper::staff);
    }

    @QueryMapping
    public List<EquipmentItemDto> pendingEquipment(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        // Fit-scored against the player's build in WorldService (needs the world), so no ApiMapper step.
        return worldService.pendingEquipmentOffers(owner, id);
    }

    @QueryMapping
    public List<EquipmentItemDto> playerEquipment(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return worldService.playerEquipment(owner, id);
    }

    @QueryMapping
    public List<EquipmentItemDto> playerLoadout(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return worldService.playerLoadout(owner, id);
    }

    @QueryMapping
    public List<EquipmentDealDto> pendingEquipmentDeals(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return worldService.pendingEquipmentDeals(owner, id);
    }

    @QueryMapping
    public EquipmentDealDto activeEquipmentDeal(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return null;
        }
        return worldService.activeEquipmentDeal(owner, id);
    }

    @QueryMapping
    public ShotSituationDto currentSituation(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPendingEvent(owner, id)) {
            return null;
        }
        // A pending event that has been played to the end has no current shot (awaiting completeEvent):
        // the service returns null, which maps to a null situation rather than being mapped as a shot.
        var situation = worldService.currentSituation(owner, id);
        return situation == null ? null : ApiMapper.situation(situation);
    }

    @QueryMapping
    public CurrentEventDto currentEvent(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPendingEvent(owner, id)) {
            return null;
        }
        return worldService.currentEvent(owner, id);
    }

    @QueryMapping
    public List<LeaderboardRowDto> eventLeaderboard(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPendingEvent(owner, id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.eventLeaderboard(owner, id), ApiMapper::leaderboardRow);
    }

    @QueryMapping
    public RoundScorecardDto playerScorecard(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPendingEvent(owner, id)) {
            return null;
        }
        // Null when the pending event has no round in progress (a playoff, or played to the end).
        var scorecard = worldService.currentScorecard(owner, id);
        return scorecard == null ? null : ApiMapper.scorecard(scorecard);
    }

    @QueryMapping
    public Boolean playerMadeCut(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPendingEvent(owner, id)) {
            return null;
        }
        return worldService.playerMadeCut(owner, id);
    }

    @QueryMapping
    public Double playerPressure(@Argument String id) {
        // Null off-event; WorldService also guards internally on a pending event.
        return worldService.playerPressure(AuthenticatedUser.requireId(), id);
    }

    @QueryMapping
    public List<SaveDto> listSaves() {
        return ApiMapper.mapList(worldService.listSaves(AuthenticatedUser.requireId()), ApiMapper::save);
    }
}
