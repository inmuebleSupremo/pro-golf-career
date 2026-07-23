package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.DevelopmentPoints;
import com.progolf.sim.tour.TourTier;
import java.util.EnumMap;
import java.util.Map;
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

    /** Spends the player's whole Development-Point bank on the cheapest raises (weakest attribute first). */
    private static void reinvest(World world, String id) {
        int budget = world.playerDevelopmentPoints();
        Attributes attrs = world.careerOf(id).player().attributes();
        Attributes potential = world.careerOf(id).player().potential();
        Map<Attribute, Integer> live = new EnumMap<>(Attribute.class);
        Map<Attribute, Integer> raises = new EnumMap<>(Attribute.class);
        for (Attribute a : Attribute.values()) {
            live.put(a, attrs.get(a));
        }
        while (true) {
            Attribute cheapest = null;
            int cheapestCost = Integer.MAX_VALUE;
            for (Attribute a : Attribute.values()) {
                if (live.get(a) >= potential.get(a)) {
                    continue;
                }
                int cost = DevelopmentPoints.costToRaise(live.get(a));
                if (cost < cheapestCost) {
                    cheapestCost = cost;
                    cheapest = a;
                }
            }
            if (cheapest == null || cheapestCost > budget) {
                break;
            }
            budget -= cheapestCost;
            live.merge(cheapest, 1, Integer::sum);
            raises.merge(cheapest, 1, Integer::sum);
        }
        if (!raises.isEmpty()) {
            world.spendDevelopmentPoints(raises);
        }
    }

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
            reinvest(world, id); // an engaged player spends the season's earned Development Points
            var tier = world.tourOf(id);
            if (tier.isPresent() && tier.get().rank() > best.rank()) {
                best = tier.get();
            }
        }

        var stats = world.careerOf(id).statistics();
        Attributes attrs = world.careerOf(id).player().attributes();
        int sum = 0;
        for (Attribute a : Attribute.values()) {
            sum += attrs.get(a);
        }
        double overall = (double) sum / Attribute.values().length;

        // Reaches the pinnacle tour during a full career...
        assertThat(best).as("best tour reached over the career").isEqualTo(TourTier.PRO);
        // ...develops to a competitive, near-ceiling level (the game once failed because the player
        // developed far too slowly to ever catch a field seeded with veterans near their ceilings)...
        assertThat(overall).as("developed overall attribute").isGreaterThanOrEqualTo(90.0);
        // ...becomes a genuine contender across the career rather than a mid-pack grinder...
        assertThat(stats.topTens()).as("career top-ten finishes").isGreaterThanOrEqualTo(15);
        // ...and wins real events. The count is kept modest deliberately: who wins among the very best is
        // high-variance (the Elite ability spread is tiny), so the robust proof of a great career is the
        // tour reached, the development, and the sustained contention above — not a precise win tally.
        assertThat(stats.wins()).as("career wins for a fully-played created golfer").isGreaterThanOrEqualTo(2);
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
