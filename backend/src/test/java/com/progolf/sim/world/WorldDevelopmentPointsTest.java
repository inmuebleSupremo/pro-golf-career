package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.player.AttributeChange;
import com.progolf.sim.progression.CareerStage;
import com.progolf.sim.progression.DevelopmentPoints;
import com.progolf.sim.progression.ProgressionEngine;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** player-development spec: the player banks Development Points from play and spends them to raise attributes. */
class WorldDevelopmentPointsTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @Test
    void onlyHumanSeasonalBaseChanges() {
        assertThat(ProgressionEngine.playerSeasonAward(18, 1.0)).isEqualTo(312);
        assertThat(DevelopmentPoints.award(CareerStage.DEVELOPMENT)).isEqualTo(195);
    }

    @Test
    void purchasesAreCumulativeAtomicAndBankable() {
        World world = World.create(81L, SMALL);
        String id = world.createPlayer("Dev", "Star", Nationality.USA, 18, Archetype.ALL_ROUNDER);
        world.advanceSeason();
        Attribute target = Attribute.PUTTING_ACCURACY;
        Attribute other = Attribute.WEDGES;
        int before = world.careerOf(id).player().attributes().get(target);

        assertThatThrownBy(() -> world.spendDevelopmentPoints(Map.of(target, 33)))
                .as("an ordinary attribute cannot become elite in one season")
                .isInstanceOf(IllegalArgumentException.class);
        world.spendDevelopmentPoints(Map.of(target, 3));
        world.spendDevelopmentPoints(Map.of(target, 3));
        assertThat(world.careerOf(id).player().attributes().get(target)).isEqualTo(before + 6);
        assertThat(world.playerDevelopmentOptions().stream().filter(o -> o.attribute() == target).findFirst()
                .orElseThrow().remainingSeasonal()).isZero();
        int bank = world.playerDevelopmentPoints();
        int otherBefore = world.careerOf(id).player().attributes().get(other);
        assertThatThrownBy(() -> world.spendDevelopmentPoints(Map.of(target, 1, other, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(world.playerDevelopmentPoints()).isEqualTo(bank);
        assertThat(world.careerOf(id).player().attributes().get(other)).isEqualTo(otherBefore);
        world.spendDevelopmentPoints(Map.of(other, 1));
        assertThat(world.careerOf(id).player().attributes().get(other)).isEqualTo(otherBefore + 1);
        assertThat(world.playerDevelopmentPoints()).isPositive();
    }

    @Test
    void reloadAndAgingDoNotResetOrConsumeAllowanceButNewSeasonDoes() {
        long seed = 82L;
        World world = World.create(seed, SMALL);
        String id = world.createPlayer("Dev", "Star", Nationality.USA, 18, Archetype.ALL_ROUNDER);
        world.advanceSeason();
        Attribute target = Attribute.DRIVING_ACCURACY;
        world.spendDevelopmentPoints(Map.of(target, 3));
        var player = world.careerOf(id).player();
        player.evolveAttributes(player.attributes().with(target, player.attributes().get(target) - 1),
                AttributeChange.Reason.AGING, world.currentSeason());
        assertThat(world.playerDevelopmentOptions().stream().filter(o -> o.attribute() == target).findFirst()
                .orElseThrow().remainingSeasonal()).isEqualTo(3);

        World loaded = World.restore(seed, SMALL, world.snapshot());
        assertThat(loaded.playerDevelopmentOptions()).isEqualTo(world.playerDevelopmentOptions());
        loaded.spendDevelopmentPoints(Map.of(target, 3));
        world.spendDevelopmentPoints(Map.of(target, 3));
        assertThat(loaded.snapshot()).isEqualTo(world.snapshot());
        assertThatThrownBy(() -> loaded.spendDevelopmentPoints(Map.of(target, 1)))
                .isInstanceOf(IllegalArgumentException.class);

        loaded.advanceSeason();
        world.advanceSeason();
        assertThat(loaded.snapshot()).isEqualTo(world.snapshot());
        assertThat(loaded.playerDevelopmentOptions().stream().filter(o -> o.attribute() == target).findFirst()
                .orElseThrow().remainingSeasonal()).isEqualTo(6);
    }

    @Test
    void olderCurrentSeasonGainsAboveNewLimitAreKeptButCannotGrowFurther() {
        long seed = 83L;
        World world = World.create(seed, SMALL);
        String id = world.createPlayer("Old", "Career", Nationality.USA, 18, Archetype.ALL_ROUNDER);
        world.advanceSeason();
        var player = world.careerOf(id).player();
        Attribute target = Attribute.IRONS_ACCURACY;
        int original = player.attributes().get(target);
        player.evolveAttributes(player.attributes().with(target, original + 8),
                AttributeChange.Reason.DEVELOPMENT, world.currentSeason());
        World loaded = World.restore(seed, SMALL, world.snapshot());
        assertThat(loaded.careerOf(id).player().attributes().get(target)).isEqualTo(original + 8);
        assertThat(loaded.playerDevelopmentOptions().stream().filter(o -> o.attribute() == target).findFirst()
                .orElseThrow().remainingSeasonal()).isZero();
        assertThatThrownBy(() -> loaded.spendDevelopmentPoints(Map.of(target, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

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
