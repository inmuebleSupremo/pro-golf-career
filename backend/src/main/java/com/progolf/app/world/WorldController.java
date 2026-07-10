package com.progolf.app.world;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A provisional, read-only HTTP surface for a world session's status (spec: world-session, design D4).
 * The tech stack makes GraphQL the primary API; this endpoint exists only to prove the engine wrapper
 * end-to-end and exposes no gameplay mutation. Health is served by Spring Boot Actuator.
 */
@RestController
@RequestMapping("/api/world")
public class WorldController {

    private final WorldService worldService;

    public WorldController(WorldService worldService) {
        this.worldService = worldService;
    }

    /** The current status (season, week, active population) of a world session. */
    @GetMapping("/{id}")
    public WorldStatus status(@PathVariable String id) {
        return worldService.status(id);
    }
}
