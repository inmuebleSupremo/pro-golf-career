package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.control.CareerGoal;
import com.progolf.sim.control.GoalType;
import com.progolf.sim.media.NewsEvent;
import com.progolf.sim.media.NewsType;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import java.util.List;
import org.junit.jupiter.api.Test;

/** career-goals spec: self-chosen ambitions that surface progress and never gate play. */
class WorldCareerGoalsTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    private static World worldWithPlayer(long seed) {
        World world = World.create(seed, small());
        world.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);
        return world;
    }

    @Test
    void goalsAreSetAndSurfacedAsProgress() {
        World world = worldWithPlayer(1L);
        world.setCareerGoals(List.of(
                CareerGoal.of(GoalType.WORLD_NUMBER_ONE),
                CareerGoal.of(GoalType.CAREER_WINS, 5)));

        List<CareerGoalProgress> progress = world.careerGoals();
        assertThat(progress).hasSize(2);
        // A brand-new created golfer has achieved neither.
        assertThat(progress).allMatch(p -> !p.achieved());
        CareerGoalProgress wins = progress.stream()
                .filter(p -> p.goal().type() == GoalType.CAREER_WINS).findFirst().orElseThrow();
        assertThat(wins.current()).isZero();
        assertThat(wins.target()).isEqualTo(5);
    }

    @Test
    void careerGoalsRequireAPlayer() {
        World world = World.create(2L, small());
        assertThatThrownBy(world::careerGoals).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reachingAGoalIsAnnouncedOnceInTheNarrative() {
        // Assign the strongest golfer (an Elite-tour regular) and set REACH_TOP_TOUR — achieved immediately,
        // so the announcement is deterministic.
        World world = World.create(3L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        world.setCareerGoals(List.of(CareerGoal.of(GoalType.REACH_TOP_TOUR)));
        assertThat(world.careerGoals().get(0).achieved()).isTrue(); // already at the top tier

        world.advanceSeason(); // the goal check fires during resolution
        assertThat(world.newsFeed().stream().filter(n -> n.type() == NewsType.GOAL_ACHIEVED).count())
                .isEqualTo(1);

        // Advancing further never re-announces the same goal.
        world.advanceSeason();
        world.advanceSeason();
        assertThat(world.newsFeed().stream().filter(n -> n.type() == NewsType.GOAL_ACHIEVED).count())
                .isEqualTo(1);
    }

    @Test
    void goalsNeverGateOrChangeCompetitiveOutcomes() {
        // Two identical worlds; in one the (existing, designated) player sets goals. Competitive outcomes
        // must match exactly — goals are purely observational.
        World withGoals = World.create(9L, small());
        World without = World.create(9L, small());
        String id = withGoals.activeGolferIds().get(0);
        withGoals.assignPlayer(id);
        without.assignPlayer(without.activeGolferIds().get(0));
        withGoals.setCareerGoals(List.of(
                CareerGoal.of(GoalType.REACH_TOP_TOUR),
                CareerGoal.of(GoalType.WIN_A_MAJOR),
                CareerGoal.of(GoalType.CAREER_WINS, 3)));

        for (int i = 0; i < 4; i++) {
            withGoals.advanceSeason();
            without.advanceSeason();
        }

        // Rankings, careers, and records are identical; only the narrative may differ (goal news).
        assertThat(withGoals.currentRanking()).isEqualTo(without.currentRanking());
        assertThat(withGoals.records()).isEqualTo(without.records());
        assertThat(withGoals.careerStatisticsOf(id).wins()).isEqualTo(without.careerStatisticsOf(id).wins());

        // The only permissible narrative difference is the extra goal-achievement news.
        List<NewsEvent> goalsFeedNonGoal = withGoals.newsFeed().stream()
                .filter(n -> n.type() != NewsType.GOAL_ACHIEVED).toList();
        assertThat(goalsFeedNonGoal).isEqualTo(without.newsFeed());
    }
}
