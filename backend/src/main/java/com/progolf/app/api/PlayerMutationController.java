package com.progolf.app.api;

import com.progolf.app.api.dto.AttributeRaiseInput;
import com.progolf.app.api.dto.CareerGoalInput;
import com.progolf.app.auth.AuthenticatedUser;
import com.progolf.app.world.WorldService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * The GraphQL player-control mutations (capability graphql-api): the human's standing management decisions,
 * delegating to {@link WorldService}. Enum-valued arguments arrive as engine enum names and are parsed by
 * {@link ApiMapper} (a bad name surfaces as a BAD_REQUEST error). Every operation is scoped to the
 * authenticated user (spec: resource-ownership) — the owner comes from the token, never from client input.
 * {@code createPlayer} returns the new golfer id; the other decisions return {@code true} on success.
 */
@Controller
public class PlayerMutationController {

    private final WorldService worldService;

    public PlayerMutationController(WorldService worldService) {
        this.worldService = worldService;
    }

    @MutationMapping
    public boolean assignPlayer(@Argument String id, @Argument String golferId) {
        worldService.assignPlayer(AuthenticatedUser.requireId(), id, golferId);
        return true;
    }

    @MutationMapping
    public String createPlayer(@Argument String id, @Argument String firstName, @Argument String lastName,
                               @Argument String nationality, @Argument int startAge, @Argument String archetype) {
        return worldService.createPlayer(AuthenticatedUser.requireId(), id, firstName, lastName,
                ApiMapper.nationality(nationality), startAge, ApiMapper.archetype(archetype));
    }

    @MutationMapping
    public boolean setDevelopmentFocus(@Argument String id, @Argument List<String> focus) {
        worldService.setDevelopmentFocus(AuthenticatedUser.requireId(), id, ApiMapper.attributes(focus));
        return true;
    }

    @MutationMapping
    public int spendDevelopmentPoints(@Argument String id, @Argument List<AttributeRaiseInput> raises) {
        return worldService.spendDevelopmentPoints(AuthenticatedUser.requireId(), id, ApiMapper.raises(raises));
    }

    @MutationMapping
    public boolean setResting(@Argument String id, @Argument boolean resting) {
        worldService.setResting(AuthenticatedUser.requireId(), id, resting);
        return true;
    }

    @MutationMapping
    public boolean skipEvent(@Argument String id, @Argument long tournamentId) {
        worldService.skipEvent(AuthenticatedUser.requireId(), id, tournamentId);
        return true;
    }

    @MutationMapping
    public boolean enterEvent(@Argument String id, @Argument long tournamentId) {
        worldService.enterEvent(AuthenticatedUser.requireId(), id, tournamentId);
        return true;
    }

    @MutationMapping
    public boolean setCareerGoals(@Argument String id, @Argument List<CareerGoalInput> goals) {
        worldService.setCareerGoals(AuthenticatedUser.requireId(), id, ApiMapper.careerGoals(goals));
        return true;
    }

    @MutationMapping
    public boolean acceptSponsorship(@Argument String id, @Argument int index) {
        worldService.acceptSponsorship(AuthenticatedUser.requireId(), id, index);
        return true;
    }

    @MutationMapping
    public boolean hireStaff(@Argument String id, @Argument int index) {
        worldService.hireStaff(AuthenticatedUser.requireId(), id, index);
        return true;
    }

    @MutationMapping
    public boolean releaseStaff(@Argument String id, @Argument String role) {
        worldService.releaseStaff(AuthenticatedUser.requireId(), id, ApiMapper.staffRole(role));
        return true;
    }

    @MutationMapping
    public boolean buyEquipment(@Argument String id, @Argument int index) {
        worldService.buyEquipment(AuthenticatedUser.requireId(), id, index);
        return true;
    }

    @MutationMapping
    public boolean selectLoadoutItem(@Argument String id, @Argument String category, @Argument String name) {
        worldService.selectLoadoutItem(AuthenticatedUser.requireId(), id, ApiMapper.equipmentCategory(category), name);
        return true;
    }
}
