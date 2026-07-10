package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.tour.TourTier;
import org.junit.jupiter.api.Test;

/** golfer-creation / player-control (modified): the player creates their own golfer to own from the start. */
class WorldCreatePlayerTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void createPlayerBuildsDesignatesAndEntersAtTheBottomTier() {
        World world = World.create(1L, small());
        int populationBefore = world.activePopulationSize();

        String id = world.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.SHORT_GAME_ARTIST);

        assertThat(world.playerGolferId()).hasValue(id);
        assertThat(world.activePopulationSize()).isEqualTo(populationBefore + 1); // a new golfer joined
        assertThat(world.tourOf(id)).hasValue(TourTier.DEVELOPMENT); // starts at the bottom
        assertThat(world.careerOf(id).player().identity().fullName()).isEqualTo("Ana Rivera");
        assertThat(world.careerOf(id).player().identity().archetype()).isEqualTo(Archetype.SHORT_GAME_ARTIST);
        assertThat(world.careerOf(id).age()).isEqualTo(20);
        assertThat(world.financialAccountOf(id)).isNotNull();
        assertThat(world.physicalStateOf(id)).isNotNull();
        assertThat(world.supportTeamOf(id)).isNotNull();
    }

    @Test
    void theCreatedPlayerCanCompete() {
        World world = World.create(2L, small());
        String id = world.createPlayer("Sam", "Okoye", Nationality.GBR, 19, Archetype.POWER_HITTER);

        // Advance a season, simming any events the player enters — they play in Development events.
        world.advanceSeason();
        while (world.hasPendingPlayerEvent()) {
            world.playerEvent().simEvent();
            world.completePlayerEvent();
        }
        assertThat(world.careerStatisticsOf(id).events()).isGreaterThan(0);
    }

    @Test
    void onlyOnePlayerMayBeCreatedOrAssigned() {
        World world = World.create(3L, small());
        world.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);

        assertThatThrownBy(() -> world.createPlayer("Bo", "Lin", Nationality.KOR, 21, Archetype.PRECISION_PLAYER))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> world.assignPlayer(world.activeGolferIds().get(0)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createPlayerIsDeterministic() {
        World a = World.create(7L, small());
        World b = World.create(7L, small());
        String idA = a.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.MENTAL_FORTRESS);
        String idB = b.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.MENTAL_FORTRESS);
        assertThat(idA).isEqualTo(idB);
        var attrsA = a.careerOf(idA).player().attributes();
        var attrsB = b.careerOf(idB).player().attributes();
        for (com.progolf.sim.core.Attribute at : com.progolf.sim.core.Attribute.values()) {
            assertThat(attrsA.get(at)).isEqualTo(attrsB.get(at));
        }
    }
}
