package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * world-session spec: the application boots, the WorldService wraps the engine, and sessions are independent
 * and owner-scoped. The status HTTP surface is now GraphQL (see WorldGraphQlApiTest); the provisional REST
 * endpoint is removed.
 */
@SpringBootTest
class WorldApplicationTest {

    private static final String OWNER = "owner-app-test";

    @Autowired
    private WorldService worldService;

    @Test
    void contextLoadsAndServiceIsWired() {
        assertThat(worldService).isNotNull();
    }

    @Test
    void aSessionIsCreatedAdvancedAndRead() {
        WorldSession session = worldService.create(OWNER, 12345L);
        assertThat(worldService.status(OWNER, session.id()).season()).isEqualTo(1);
        assertThat(worldService.status(OWNER, session.id()).activePopulation()).isGreaterThan(0);

        worldService.advanceSeason(OWNER, session.id());
        assertThat(worldService.status(OWNER, session.id()).season()).isEqualTo(2);
    }

    @Test
    void sessionsAreIndependent() {
        WorldSession a = worldService.create(OWNER, 1L);
        WorldSession b = worldService.create(OWNER, 2L);
        worldService.advanceSeason(OWNER, a.id());

        assertThat(worldService.status(OWNER, a.id()).season()).isEqualTo(2);
        assertThat(worldService.status(OWNER, b.id()).season()).isEqualTo(1); // untouched
    }

    @Test
    void aSessionIsReachableOnlyByItsOwner() {
        WorldSession session = worldService.create(OWNER, 55L);
        assertThat(worldService.status(OWNER, session.id()).season()).isEqualTo(1);

        // A different user addressing the same id sees nothing (not-found), not the session.
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> worldService.status("someone-else", session.id()))
                .isInstanceOf(WorldSessionNotFoundException.class);
    }
}
