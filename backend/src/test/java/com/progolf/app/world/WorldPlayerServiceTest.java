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

    @Autowired
    private WorldService worldService;

    @Test
    void aPlayerTakesATurnThroughTheService() {
        WorldSession session = worldService.create(42L);
        String golferId = session.world().activeGolferIds().get(0);

        // Configure the player's golfer, then advance.
        worldService.assignPlayer(session.id(), golferId);
        worldService.setDevelopmentFocus(session.id(), List.of(Attribute.PUTTING_ACCURACY));
        worldService.setResting(session.id(), false);
        worldService.advanceSeason(session.id());

        // Offers await the player's decision; accepting one signs it.
        assertThat(worldService.pendingSponsorships(session.id())).isNotEmpty();
        int before = worldService.pendingSponsorships(session.id()).size();
        worldService.acceptSponsorship(session.id(), 0);
        assertThat(worldService.pendingSponsorships(session.id())).hasSize(before - 1);

        // The turn advanced the world.
        assertThat(worldService.status(session.id()).season()).isEqualTo(2);
    }
}
