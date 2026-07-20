package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.CourseSetup;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tournament.SetupDifficulty;
import com.progolf.sim.tournament.Tier;
import org.junit.jupiter.api.Test;

/**
 * Guards the risk/reward balance of the three strategies (spec: shot-resolution / tournament-play). Strategy
 * must be a genuine choice — aggression trades variance, not a free lower score — so no disposition strictly
 * dominates another on mean.
 *
 * <p>This exists because it once failed badly: pin-attacking handed AGGRESSIVE a ~1.75 strokes/round mean
 * advantage over BALANCED, which made it strictly best. That both let the AI field post absurd winning scores
 * and quietly doomed the human player, who is locked to BALANCED when no risk approach is chosen — an equal
 * golfer playing the dominated strategy could never keep up.
 */
class StrategyBalanceTest {

    private static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(4242L, 3, 0, 0, 0, 0, 0),
                EnvironmentClassification.PARKLAND);
    }

    private static double meanRound(Course c, CourseSetup setup, int rating, Strategy strat, int rounds) {
        Attributes attrs = Attributes.uniform(rating);
        GolferState state = new GolferState(0.35, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        double total = 0;
        for (int r = 0; r < rounds; r++) {
            for (int h = 1; h <= 18; h++) {
                total += RoundResolver.resolveHole(c.holeModel(h, 1, setup), attrs, state, Environment.calm(),
                        strat, new SeedCoordinate(999L, 2, rating, r, h, 9, 0)).totalStrokes();
            }
        }
        return total / rounds;
    }

    @Test
    void noStrategyStrictlyDominatesOnMeanScore() {
        Course c = course();
        int rounds = 200;
        CourseSetup setup = SetupDifficulty.forEvent(Tier.PREMIER, EventPrestige.REGULAR);
        int rating = 88; // a strong golfer, who exploits pin-attacking the most

        double conservative = meanRound(c, setup, rating, Strategy.CONSERVATIVE, rounds);
        double balanced = meanRound(c, setup, rating, Strategy.BALANCED, rounds);
        double aggressive = meanRound(c, setup, rating, Strategy.AGGRESSIVE, rounds);

        // The three strategies' mean scores must sit within about a stroke a round of each other: the choice
        // is variance, not free strokes. In particular AGGRESSIVE must not be materially better than BALANCED,
        // or the human default (BALANCED) is a permanent handicap and the field's aggressive golfers run away.
        double spread = Math.max(Math.max(conservative, balanced), aggressive)
                - Math.min(Math.min(conservative, balanced), aggressive);
        assertThat(spread).as("mean-score spread across strategies (strokes/round)").isLessThan(1.0);
        assertThat(aggressive - balanced)
                .as("aggressive's mean advantage over balanced (strokes/round)")
                .isLessThan(0.5);
    }
}
