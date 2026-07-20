package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.tour.TourTier;
import org.junit.jupiter.api.Test;

/**
 * Guards the player's career arc (spec: player-development / golfer-creation): a created golfer who plays a
 * full career must be able to become a genuine contender — reach the top tour and win real events — rather
 * than grind out a winless mid-pack existence.
 *
 * <p>This exists because the whole game once failed it: across 20+ seasons the player never won a single
 * event and never left the lower tours, because they were resolved on a dominated strategy, developed far
 * too slowly to ever catch a field seeded with veterans near their ceilings, and had a ceiling below the
 * field's best. A career mode has to offer a path to greatness; this asserts one exists.
 */
class WorldPlayerSuccessTest {

    @Test
    void aDevelopedCareerReachesTheTopTourAndWins() {
        World world = World.create(42L);
        String id = world.createPlayer("Star", "Prospect", Nationality.GBR, 18, Archetype.ALL_ROUNDER);

        TourTier best = TourTier.DEVELOPMENT;
        for (int s = 1; s <= 16; s++) {
            world.advanceSeason();
            if (world.careerOf(id).isRetired()) {
                break;
            }
            var tier = world.tourOf(id);
            if (tier.isPresent() && tier.get().rank() > best.rank()) {
                best = tier.get();
            }
        }

        // Reaches the pinnacle tour during a full career...
        assertThat(best).as("best tour reached over the career").isEqualTo(TourTier.ELITE);
        // ...and wins real events on merit — a superstar career, not a winless one.
        assertThat(world.careerOf(id).statistics().wins())
                .as("career wins for a fully-played created golfer").isGreaterThanOrEqualTo(5);
    }

    @Test
    void theGeneratedFieldStillWinsRealisticallyAmongItself() {
        // The player's talent edge must not have leaked into the AI population: an ordinary generated golfer
        // is not on a superstar arc, so most of the field never wins, exactly as in real professional golf.
        World world = World.create(9L);
        for (int s = 1; s <= 8; s++) {
            world.advanceSeason();
        }
        long winlessGolfers = world.activeGolferIds().stream()
                .filter(g -> world.careerOf(g).statistics().wins() == 0)
                .count();
        long total = world.activeGolferIds().size();
        assertThat((double) winlessGolfers / total)
                .as("fraction of the AI field that has never won").isGreaterThan(0.5);
    }
}
