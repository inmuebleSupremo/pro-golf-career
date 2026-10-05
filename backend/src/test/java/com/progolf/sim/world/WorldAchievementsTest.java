package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.achievement.Achievement;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** career-achievements: the catalogue is surfaced per player, unlocked once, and persists across a save. */
class WorldAchievementsTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void autonomousWorldHasNoPlayerAchievements() {
        World world = World.create(1L, small());
        assertThat(world.playerAchievements()).isEmpty();
    }

    @Test
    void aNewPlayerSeesTheWholeCatalogueAllLocked() {
        World world = World.create(1L, small());
        world.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);

        Map<Achievement, Integer> achievements = world.playerAchievements();
        assertThat(achievements).containsOnlyKeys(Achievement.values());
        // A brand-new golfer has unlocked none — every season stamp is null (locked).
        assertThat(achievements.values()).containsOnlyNulls();
    }

    @Test
    void playerAchievementCatalogueSurvivesASaveAndRestore() {
        World original = World.create(2026L, small());
        original.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);

        World restored = World.restore(2026L, small(), original.snapshot());
        assertThat(restored.playerAchievements()).isEqualTo(original.playerAchievements());
    }
}
