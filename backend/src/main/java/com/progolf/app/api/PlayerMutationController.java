package com.progolf.app.api;

import com.progolf.app.api.dto.CareerGoalInput;
import com.progolf.app.world.WorldService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * The GraphQL player-control mutations (capability graphql-api): the human's standing management decisions,
 * delegating to {@link WorldService}. Enum-valued arguments arrive as engine enum names and are parsed by
 * {@link ApiMapper} (a bad name surfaces as a BAD_REQUEST error). {@code createPlayer} returns the new
 * golfer id; the other decisions return {@code true} on success and the client re-queries the read model.
 */
@Controller
public class PlayerMutationController {

    private final WorldService worldService;

    public PlayerMutationController(WorldService worldService) {
        this.worldService = worldService;
    }

    @MutationMapping
    public boolean assignPlayer(@Argument String id, @Argument String golferId) {
        worldService.assignPlayer(id, golferId);
        return true;
    }

    @MutationMapping
    public String createPlayer(@Argument String id, @Argument String firstName, @Argument String lastName,
                               @Argument String nationality, @Argument int startAge, @Argument String archetype) {
        return worldService.createPlayer(id, firstName, lastName,
                ApiMapper.nationality(nationality), startAge, ApiMapper.archetype(archetype));
    }

    @MutationMapping
    public boolean setDevelopmentFocus(@Argument String id, @Argument List<String> focus) {
        worldService.setDevelopmentFocus(id, ApiMapper.attributes(focus));
        return true;
    }

    @MutationMapping
    public boolean setResting(@Argument String id, @Argument boolean resting) {
        worldService.setResting(id, resting);
        return true;
    }

    @MutationMapping
    public boolean skipEvent(@Argument String id, @Argument long tournamentId) {
        worldService.skipEvent(id, tournamentId);
        return true;
    }

    @MutationMapping
    public boolean enterEvent(@Argument String id, @Argument long tournamentId) {
        worldService.enterEvent(id, tournamentId);
        return true;
    }

    @MutationMapping
    public boolean setCareerGoals(@Argument String id, @Argument List<CareerGoalInput> goals) {
        worldService.setCareerGoals(id, ApiMapper.careerGoals(goals));
        return true;
    }

    @MutationMapping
    public boolean acceptSponsorship(@Argument String id, @Argument int index) {
        worldService.acceptSponsorship(id, index);
        return true;
    }

    @MutationMapping
    public boolean hireStaff(@Argument String id, @Argument int index) {
        worldService.hireStaff(id, index);
        return true;
    }

    @MutationMapping
    public boolean releaseStaff(@Argument String id, @Argument String role) {
        worldService.releaseStaff(id, ApiMapper.staffRole(role));
        return true;
    }

    @MutationMapping
    public boolean buyEquipment(@Argument String id, @Argument int index) {
        worldService.buyEquipment(id, index);
        return true;
    }
}
