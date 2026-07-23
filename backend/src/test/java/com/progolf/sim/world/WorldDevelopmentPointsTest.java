package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** player-development spec: the player banks Development Points from play and spends them to raise attributes. */
class WorldDevelopmentPointsTest {

    @Test
    void aSeasonBanksPointsThatSpendToRaiseAttributes() {
        World world = World.create(7L);
        String id = world.createPlayer("Dev", "Star", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        assertThat(world.playerDevelopmentPoints()).as("nothing banked before a season").isZero();

        world.advanceSeason();
        int banked = world.playerDevelopmentPoints();
        assertThat(banked).as("play banks Development Points").isPositive();

        int before = world.careerOf(id).player().attributes().get(Attribute.PUTTING_ACCURACY);
        world.spendDevelopmentPoints(Map.of(Attribute.PUTTING_ACCURACY, 3));

        assertThat(world.careerOf(id).player().attributes().get(Attribute.PUTTING_ACCURACY))
                .as("the attribute rose by the spent amount").isEqualTo(before + 3);
        assertThat(world.playerDevelopmentPoints()).as("points were debited").isLessThan(banked);
    }

    @Test
    void spendingMoreThanBankedIsRejectedAndChangesNothing() {
        World world = World.create(8L);
        String id = world.createPlayer("Dev", "Star", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        world.advanceSeason();

        int banked = world.playerDevelopmentPoints();
        int drivingBefore = world.careerOf(id).player().attributes().get(Attribute.DRIVING_DISTANCE);

        // Raise every attribute far beyond one season's budget (clamped to potential, but still unaffordable).
        Map<Attribute, Integer> tooMuch = new EnumMap<>(Attribute.class);
        for (Attribute a : Attribute.values()) {
            tooMuch.put(a, 50);
        }
        assertThatThrownBy(() -> world.spendDevelopmentPoints(tooMuch))
                .isInstanceOf(IllegalArgumentException.class);

        // Rejected atomically: nothing spent, nothing changed.
        assertThat(world.playerDevelopmentPoints()).isEqualTo(banked);
        assertThat(world.careerOf(id).player().attributes().get(Attribute.DRIVING_DISTANCE))
                .isEqualTo(drivingBefore);
    }
}
