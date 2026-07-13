package com.progolf.app.api;

import com.progolf.app.api.dto.WorldStatusDto;
import com.progolf.app.world.WorldService;
import com.progolf.app.world.WorldSession;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * The GraphQL persistence-write mutations (capability graphql-api): save a session, load a save into a new
 * session (returning its status so the client can continue with the new id), and delete a save. An unknown
 * save id surfaces as a NOT_FOUND error.
 */
@Controller
public class PersistenceMutationController {

    private final WorldService worldService;

    public PersistenceMutationController(WorldService worldService) {
        this.worldService = worldService;
    }

    @MutationMapping
    public boolean save(@Argument String id, @Argument String saveId) {
        worldService.save(id, saveId);
        return true;
    }

    @MutationMapping
    public WorldStatusDto load(@Argument String saveId) {
        WorldSession session = worldService.load(saveId);
        return worldService.status(session.id());
    }

    @MutationMapping
    public boolean deleteSave(@Argument String saveId) {
        worldService.deleteSave(saveId);
        return true;
    }
}
