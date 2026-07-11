package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.career.CareerConstants;
import com.progolf.sim.career.HallOfFameInduction;
import com.progolf.sim.media.NewsEvent;
import com.progolf.sim.media.NewsType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** career-legacy: the World's biennial Hall-of-Fame election — cadence, selectivity, news, reproducibility. */
class WorldHallOfFameTest {

    private static final int SEASONS = 34; // long enough for the first legends to be inducted (~season 28)

    private static WorldConfig config() {
        return new WorldConfig(60, 8, 4, 30, 4);
    }

    private static World runWorld(long seed) {
        World world = World.create(seed, config());
        for (int s = 0; s < SEASONS; s++) {
            world.advanceSeason();
        }
        return world;
    }

    /** The lowest score any baseline-eligible candidate can have (2 majors + 13 regular pro wins). */
    private static double minBaselineScore() {
        return CareerConstants.HOF_SCORE_MAJOR * CareerConstants.HOF_MIN_MAJORS
                + CareerConstants.HOF_SCORE_REGULAR * (CareerConstants.HOF_MIN_PRO_WINS - CareerConstants.HOF_MIN_MAJORS);
    }

    @Test
    void electionsAreBiennialAndInductAtMostOneCandidatePerCycle() {
        World world = runWorld(2026L);
        var inductions = world.hallOfFameInductions();
        assertThat(inductions).as("legends should be inducted over %d seasons", SEASONS).isNotEmpty();

        Set<Integer> seasonsSeen = new HashSet<>();
        for (HallOfFameInduction in : inductions) {
            // Cadence: inductions happen only on an election-cycle season.
            assertThat(in.season() % CareerConstants.HOF_ELECTION_CYCLE_SEASONS).isZero();
            // Selectivity: at most one inductee per cycle.
            assertThat(seasonsSeen.add(in.season())).as("only one induction in season %d", in.season()).isTrue();
            // Every inductee cleared the statistical baseline (>= 2 majors + >= 15 pro wins).
            assertThat(in.score()).isGreaterThanOrEqualTo(minBaselineScore());
        }
    }

    @Test
    void inductedGolfersAreHallOfFameMembersAndAnnounced() {
        World world = runWorld(2026L);
        for (HallOfFameInduction in : world.hallOfFameInductions()) {
            assertThat(world.isInHallOfFame(in.golferId())).isTrue();
            assertThat(world.hallOfFameMembers()).contains(in.golferId());
        }
        // A never-created id is not a member.
        assertThat(world.isInHallOfFame("nobody")).isFalse();

        // Each induction is announced as the pinnacle news event.
        long inductionNews = world.newsFeed().stream()
                .filter(n -> n.type() == NewsType.HALL_OF_FAME_INDUCTION).count();
        assertThat(inductionNews).isEqualTo(world.hallOfFameInductions().size());
        for (NewsEvent n : world.newsFeed()) {
            if (n.type() == NewsType.HALL_OF_FAME_INDUCTION) {
                assertThat(n.isSignificant()).isTrue();
            }
        }
    }

    @Test
    void electionsAreReproducibleFromTheSameSeed() {
        assertThat(runWorld(7L).hallOfFameInductions())
                .isEqualTo(runWorld(7L).hallOfFameInductions());
    }
}
