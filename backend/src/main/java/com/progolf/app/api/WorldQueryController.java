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
    public List<CareerGoalDto> careerGoals(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.careerGoals(owner, id), ApiMapper::careerGoal);
    }

    @QueryMapping
    public List<HallOfFameDto> hallOfFame(@Argument String id) {
        return ApiMapper.mapList(worldService.hallOfFame(AuthenticatedUser.requireId(), id), ApiMapper::hallOfFame);
    }

    @QueryMapping
    public List<SponsorshipOfferDto> pendingSponsorships(@Argument String id) {
        return ApiMapper.mapList(worldService.pendingSponsorships(AuthenticatedUser.requireId(), id),
                ApiMapper::sponsorship);
    }

    @QueryMapping
    public List<StaffMemberDto> pendingStaff(@Argument String id) {
        return ApiMapper.mapList(worldService.pendingStaffOffers(AuthenticatedUser.requireId(), id), ApiMapper::staff);
    }

    @QueryMapping
    public List<EquipmentItemDto> pendingEquipment(@Argument String id) {
        return ApiMapper.mapList(worldService.pendingEquipmentOffers(AuthenticatedUser.requireId(), id),
                ApiMapper::equipment);
    }

    @QueryMapping
    public List<EquipmentItemDto> playerEquipment(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.playerEquipment(owner, id), ApiMapper::equipment);
    }

    @QueryMapping
    public List<EquipmentItemDto> playerLoadout(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPlayer(owner, id)) {
            return List.of();
        }
        return ApiMapper.mapList(worldService.playerLoadout(owner, id), ApiMapper::equipment);
    }

    @QueryMapping
    public ShotSituationDto currentSituation(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPendingEvent(owner, id)) {
            return null;
        }
        return ApiMapper.situation(worldService.currentSituation(owner, id));
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
    public Boolean playerMadeCut(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        if (!worldService.hasPendingEvent(owner, id)) {
            return null;
        }
        return worldService.playerMadeCut(owner, id);
    }

    @QueryMapping
    public List<SaveDto> listSaves() {
        return ApiMapper.mapList(worldService.listSaves(AuthenticatedUser.requireId()), ApiMapper::save);
    }
}
