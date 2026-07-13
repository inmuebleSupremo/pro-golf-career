package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** player-control spec: a human plays a turn through the application's service boundary. */
@SpringBootTest
class WorldPlayerServiceTest {

    private static final String OWNER = "owner-player-test";

    @Autowired
    private WorldService worldService;

    @Test
    void aPlayerTakesATurnThroughTheService() {
        WorldSession session = worldService.create(OWNER, 42L);
        String golferId = session.world().activeGolferIds().get(0);

        // Configure the player's golfer, then advance.
        worldService.assignPlayer(OWNER, session.id(), golferId);
        worldService.setDevelopmentFocus(OWNER, session.id(), List.of(Attribute.PUTTING_ACCURACY));
        worldService.setResting(OWNER, session.id(), false);
        worldService.advanceSeason(OWNER, session.id());

        // Offers await the player's decision; accepting one signs it.
        assertThat(worldService.pendingSponsorships(OWNER, session.id())).isNotEmpty();
        int before = worldService.pendingSponsorships(OWNER, session.id()).size();
        worldService.acceptSponsorship(OWNER, session.id(), 0);
        assertThat(worldService.pendingSponsorships(OWNER, session.id())).hasSize(before - 1);

        // The turn advanced the world.
        assertThat(worldService.status(OWNER, session.id()).season()).isEqualTo(2);
    }
}
