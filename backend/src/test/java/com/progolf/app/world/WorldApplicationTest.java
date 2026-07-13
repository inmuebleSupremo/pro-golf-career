package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * world-session spec: the application boots, the WorldService wraps the engine, and sessions are independent.
 * The status HTTP surface is now GraphQL (see WorldGraphQlApiTest); the provisional REST endpoint is removed.
 */
@SpringBootTest
class WorldApplicationTest {

    @Autowired
    private WorldService worldService;

    @Test
    void contextLoadsAndServiceIsWired() {
        assertThat(worldService).isNotNull();
    }

    @Test
    void aSessionIsCreatedAdvancedAndRead() {
        WorldSession session = worldService.create(12345L);
        assertThat(worldService.status(session.id()).season()).isEqualTo(1);
        assertThat(worldService.status(session.id()).activePopulation()).isGreaterThan(0);

        worldService.advanceSeason(session.id());
        assertThat(worldService.status(session.id()).season()).isEqualTo(2);
    }

    @Test
    void sessionsAreIndependent() {
        WorldSession a = worldService.create(1L);
        WorldSession b = worldService.create(2L);
        worldService.advanceSeason(a.id());

        assertThat(worldService.status(a.id()).season()).isEqualTo(2);
        assertThat(worldService.status(b.id()).season()).isEqualTo(1); // untouched
    }
}
