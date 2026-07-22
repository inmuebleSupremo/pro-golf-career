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
    void aCreatedPlayerCompetesAtScaleWhereTheFieldCutBites() {
        // A larger world: the Development tier holds more golfers than a field, so the field draw must cut.
        // Before skill-based entry (spec: competitive-entry), a freshly-created golfer had zero season points
        // and an id that sorts last, so they never entered a field, never scored, and stayed excluded. The
        // skill tie-break lets a competent entry-tier build in.
        WorldConfig large = new WorldConfig(160, 6, 3, 32, 4);
        World world = World.create(11L, large);
        String id = world.createPlayer("Ken", "Barlow", Nationality.USA, 20, Archetype.ALL_ROUNDER);

        world.advanceSeason();
        while (world.hasPendingPlayerEvent()) {
            world.playerEvent().simEvent();
            world.completePlayerEvent();
        }

        assertThat(world.careerStatisticsOf(id).events())
                .as("a created golfer of entry-tier ability should make fields even when the cut bites")
                .isGreaterThan(0);
    }

    @Test
    void aCreatedPlayerCompetesFromTheirFirstSeasonAtDefaultScale() {
        // Regression (spec: player-control): at default scale the Development tour holds far more golfers
        // than a field (640 population, ~84% on Development, 120-slot field), so a baseline created golfer
        // ranked below the standings cut used to sit out entire early seasons until training lifted them.
        // An entered event on the player's own tour must be played from season 1.
        World world = World.create(42L);
        String id = world.createPlayer("Rookie", "Debut", Nationality.USA, 20, Archetype.ALL_ROUNDER);

        world.advanceSeason();
        while (world.hasPendingPlayerEvent()) {
            world.playerEvent().simEvent();
            world.completePlayerEvent();
        }

        assertThat(world.careerStatisticsOf(id).events())
                .as("a created golfer must play events from their first season, not sit out until developed")
                .isGreaterThan(0);
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
