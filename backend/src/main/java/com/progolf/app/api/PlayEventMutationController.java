package com.progolf.app.api;

import com.progolf.app.api.dto.ShotDecisionInput;
import com.progolf.app.api.dto.ShotOutcomeDto;
import com.progolf.app.api.dto.WorldStatusDto;
import com.progolf.app.world.WorldService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * The GraphQL playable-event mutations (capability graphql-api): play or sim the player's paused event.
 * Playing/simming a shot returns the resolved outcome; completing the event resumes the paused week and
 * returns the new world status. Acting off-event throws in the engine and surfaces as a BAD_REQUEST error.
 */
@Controller
public class PlayEventMutationController {

    private final WorldService worldService;

    public PlayEventMutationController(WorldService worldService) {
        this.worldService = worldService;
    }

    @MutationMapping
    public ShotOutcomeDto playShot(@Argument String id, @Argument ShotDecisionInput decision) {
        return ApiMapper.shotOutcome(worldService.playShot(id, ApiMapper.shotDecision(decision)));
    }

    @MutationMapping
    public ShotOutcomeDto simShot(@Argument String id) {
        return ApiMapper.shotOutcome(worldService.simShot(id));
    }

    @MutationMapping
    public boolean simHole(@Argument String id) {
        worldService.simHole(id);
        return true;
    }

    @MutationMapping
    public boolean simRound(@Argument String id) {
        worldService.simRound(id);
        return true;
    }

    @MutationMapping
    public boolean simEvent(@Argument String id) {
        worldService.simEvent(id);
        return true;
    }

    @MutationMapping
    public WorldStatusDto completeEvent(@Argument String id) {
        worldService.completeEvent(id);
        return worldService.status(id);
    }
}
