package com.progolf.app.api;

import com.progolf.app.api.dto.CareerGoalDto;
import com.progolf.app.api.dto.EquipmentItemDto;
import com.progolf.app.api.dto.HallOfFameDto;
import com.progolf.app.api.dto.LeaderboardRowDto;
import com.progolf.app.api.dto.SaveDto;
import com.progolf.app.api.dto.ScheduleEntryDto;
import com.progolf.app.api.dto.ShotSituationDto;
import com.progolf.app.api.dto.SponsorshipOfferDto;
import com.progolf.app.api.dto.StaffMemberDto;
import com.progolf.app.api.dto.WorldStatusDto;
import com.progolf.app.world.WorldService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * The GraphQL read model (capability graphql-api): {@code @QueryMapping} resolvers delegating to
 * {@link WorldService} and returning application DTOs (via {@link ApiMapper}) — no {@code sim.*} type is
 * exposed. Playable-event reads are safe off-event: they return null / an empty list when no player event is
 * pending, rather than raising an error.
 */
@Controller
public class WorldQueryController {

    private final WorldService worldService;

    public WorldQueryController(WorldService worldService) {
        this.worldService = worldService;
    }

    @QueryMapping
    public WorldStatusDto world(@Argument String id) {
        return worldService.status(id);
    }

    @QueryMapping
    public List<ScheduleEntryDto> playerSchedule(@Argument String id) {
        // Player-scoped: the engine requires a player; report empty rather than error when none is assigned.
        if (!worldService.hasPlayer(id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.playerSchedule(id), ApiMapper::schedule);
    }

    @QueryMapping
    public List<CareerGoalDto> careerGoals(@Argument String id) {
        if (!worldService.hasPlayer(id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.careerGoals(id), ApiMapper::careerGoal);
    }

    @QueryMapping
    public List<HallOfFameDto> hallOfFame(@Argument String id) {
        return ApiMapper.mapList(worldService.hallOfFame(id), ApiMapper::hallOfFame);
    }

    @QueryMapping
    public List<SponsorshipOfferDto> pendingSponsorships(@Argument String id) {
        return ApiMapper.mapList(worldService.pendingSponsorships(id), ApiMapper::sponsorship);
    }

    @QueryMapping
    public List<StaffMemberDto> pendingStaff(@Argument String id) {
        return ApiMapper.mapList(worldService.pendingStaffOffers(id), ApiMapper::staff);
    }

    @QueryMapping
    public List<EquipmentItemDto> pendingEquipment(@Argument String id) {
        return ApiMapper.mapList(worldService.pendingEquipmentOffers(id), ApiMapper::equipment);
    }

    @QueryMapping
    public List<EquipmentItemDto> playerEquipment(@Argument String id) {
        if (!worldService.hasPlayer(id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.playerEquipment(id), ApiMapper::equipment);
    }

    @QueryMapping
    public List<EquipmentItemDto> playerLoadout(@Argument String id) {
        if (!worldService.hasPlayer(id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.playerLoadout(id), ApiMapper::equipment);
    }

    @QueryMapping
    public ShotSituationDto currentSituation(@Argument String id) {
        if (!worldService.hasPendingEvent(id)) {
            return null;
        }
        return ApiMapper.situation(worldService.currentSituation(id));
    }

    @QueryMapping
    public List<LeaderboardRowDto> eventLeaderboard(@Argument String id) {
        if (!worldService.hasPendingEvent(id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.eventLeaderboard(id), ApiMapper::leaderboardRow);
    }

    @QueryMapping
    public Boolean playerMadeCut(@Argument String id) {
        if (!worldService.hasPendingEvent(id)) {
            return null;
        }
        return worldService.playerMadeCut(id);
    }

    @QueryMapping
    public List<SaveDto> listSaves() {
        return ApiMapper.mapList(worldService.listSaves(), ApiMapper::save);
    }
}
