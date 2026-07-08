package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Course-mastery spec: per-player, relationship-owned, isolated between courses, bounded. */
class CourseMasteryTest {

    @Test
    void masteryStartsAtZeroAndIncreasesWithClamping() {
        CourseMastery m = CourseMastery.initial("player-1", "course-a");
        assertThat(m.mastery()).isEqualTo(0.0);
        assertThat(m.increasedBy(0.3).mastery()).isEqualTo(0.3);
        assertThat(m.increasedBy(5.0).mastery()).isEqualTo(1.0); // clamped to 1.0
    }

    @Test
    void masteryDoesNotLeakBetweenCoursesOrPlayers() {
        CourseMastery pA = CourseMastery.initial("player-1", "course-a").increasedBy(0.8);
        CourseMastery pB = CourseMastery.initial("player-1", "course-b");
        CourseMastery other = CourseMastery.initial("player-2", "course-a");
        // Course B starts independent of high mastery on course A.
        assertThat(pB.mastery()).isEqualTo(0.0);
        // A second player's relationship with the same course is independent too.
        assertThat(other.mastery()).isEqualTo(0.0);
        assertThat(pA.mastery()).isEqualTo(0.8);
    }

    @Test
    void invalidMasteryIsRejected() {
        assertThatThrownBy(() -> new CourseMastery("p", "c", 1.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CourseMastery("p", "c", -0.1)).isInstanceOf(IllegalArgumentException.class);
    }
}
