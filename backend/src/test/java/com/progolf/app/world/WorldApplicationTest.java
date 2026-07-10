package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * world-session spec: the application boots, the WorldService wraps the engine, sessions are independent,
 * and a session's status is served over HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WorldApplicationTest {

    @Autowired
    private WorldService worldService;

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void statusIsServedOverHttp() throws Exception {
        WorldSession session = worldService.create(999L);
        mockMvc.perform(get("/api/world/{id}", session.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.id()))
                .andExpect(jsonPath("$.season").value(1))
                .andExpect(jsonPath("$.activePopulation").value(worldService.status(session.id()).activePopulation()));
    }

    @Test
    void unknownSessionReturns404() throws Exception {
        mockMvc.perform(get("/api/world/{id}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
