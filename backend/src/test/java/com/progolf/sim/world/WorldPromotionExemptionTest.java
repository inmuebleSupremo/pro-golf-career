package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.tour.TourTier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tour-movement spec: promotion must be a pathway, not a revolving door. At realistic scale the Pro
 * tour holds more members (~192) than an event's field can seat (120), so entry is merit-ordered — and a
 * golfer promoted for topping Development arrives as the weakest member of the tier they earned. Without a
 * card they would miss every field, score no points, and be relegated straight back without ever teeing off.
 */
class WorldPromotionExemptionTest {

    private static Map<String, TourTier> tiersOf(World world) {
        Map<String, TourTier> tiers = new HashMap<>();
        for (String id : world.activeGolferIds()) {
            world.tourOf(id).ifPresent(t -> tiers.put(id, t));
        }
        return tiers;
    }

    @Test
    void golfersPromotedIntoAnOversubscribedTourAllGetStarts() {
        World world = World.create(42L); // default scale: 640 golfers, 120-seat fields

        Map<String, TourTier> before = tiersOf(world);
        world.advanceSeason(); // season 1 plays; its review promotes the top of each tier
        Map<String, TourTier> after = tiersOf(world);

        List<String> promoted = new ArrayList<>();
        for (var e : before.entrySet()) {
            if (e.getValue() == TourTier.DEVELOPMENT && after.get(e.getKey()) == TourTier.PRO) {
                promoted.add(e.getKey());
            }
        }
        assertThat(promoted).isNotEmpty(); // the scenario under test actually occurred

        int season = world.currentSeason();
        world.advanceSeason(); // their first season up

        // Every one of them competed: the card guarantees entry despite arriving bottom of the tier.
        assertThat(promoted).allSatisfy(id ->
                assertThat(world.seasonStatisticsOf(id, season).events()).isPositive());
    }

    @Test
    void aPromotedGolferIsNotRelegatedWithoutHavingPlayed() {
        World world = World.create(42L);

        Map<String, TourTier> before = tiersOf(world);
        world.advanceSeason();
        Map<String, TourTier> after = tiersOf(world);
        world.advanceSeason();
        Map<String, TourTier> end = tiersOf(world);

        // Nobody promoted to Pro bounced back to Development having never teed off.
        for (var e : before.entrySet()) {
            String id = e.getKey();
            if (e.getValue() == TourTier.DEVELOPMENT && after.get(id) == TourTier.PRO
                    && end.get(id) == TourTier.DEVELOPMENT) {
                assertThat(world.seasonStatisticsOf(id, 2).events())
                        .as("relegated golfer %s must at least have had the chance to compete", id)
                        .isPositive();
            }
        }
    }
}
