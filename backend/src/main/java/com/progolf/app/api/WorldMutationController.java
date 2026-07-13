package com.progolf.app.api;

import com.progolf.app.api.dto.WorldConfigInput;
import com.progolf.app.api.dto.WorldStatusDto;
import com.progolf.app.auth.AuthenticatedUser;
import com.progolf.app.world.WorldService;
import com.progolf.app.world.WorldSession;
import com.progolf.sim.world.WorldConfig;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * The GraphQL world-lifecycle mutations (capability graphql-api): create a session and advance it, each
 * returning the resulting status DTO. Player / play / persistence-write mutations are added in the next
 * slice (add-graphql-mutations). Delegates to {@link WorldService}; no {@code sim.*} type is exposed.
 */
@Controller
public class WorldMutationController {

    private final WorldService worldService;

    public WorldMutationController(WorldService worldService) {
        this.worldService = worldService;
    }

    @MutationMapping
    public WorldStatusDto createWorld(@Argument long seed, @Argument WorldConfigInput config) {
        String owner = AuthenticatedUser.requireId();
        WorldConfig cfg = ApiMapper.worldConfig(config);
        WorldSession session = (cfg == null)
                ? worldService.create(owner, seed) : worldService.create(owner, seed, cfg);
        return worldService.status(owner, session.id());
    }

    @MutationMapping
    public WorldStatusDto advanceSeason(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        worldService.advanceSeason(owner, id);
        return worldService.status(owner, id);
    }

    @MutationMapping
    public WorldStatusDto advanceWeek(@Argument String id) {
        String owner = AuthenticatedUser.requireId();
        worldService.advanceWeek(owner, id);
        return worldService.status(owner, id);
    }
}
