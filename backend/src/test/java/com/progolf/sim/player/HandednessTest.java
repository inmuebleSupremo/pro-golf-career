package com.progolf.sim.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Handedness;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HandednessTest {
    @Test
    void legacy_constructor_and_snapshot_default_to_right() {
        Player player = new Player("p", new Identity("A", "B", Nationality.USA, LocalDate.of(2000, 1, 1), Archetype.ALL_ROUNDER),
                Attributes.uniform(50));
        assertEquals(Handedness.RIGHT, player.handedness());
        assertEquals(Handedness.RIGHT, Player.restore(player.snapshot()).handedness());
    }
}
