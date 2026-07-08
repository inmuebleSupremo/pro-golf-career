package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Boundary spec (REQ-083): the Course domain must carry no tournament-specific state. This asserts that
 * no Course-domain record exposes a field named for competitive concepts.
 */
class CourseBoundaryTest {

    private static final List<String> FORBIDDEN = List.of(
            "leaderboard", "prize", "ranking", "competitor", "score", "purse", "winner");

    @Test
    void courseDomainRecordsCarryNoTournamentState() {
        List<Class<?>> records = List.of(
                Course.class, CourseIdentity.class, GeneratedHole.class, PinPosition.class,
                CourseDifficulty.class, CourseMastery.class);
        for (Class<?> type : records) {
            for (RecordComponent rc : type.getRecordComponents()) {
                String name = rc.getName().toLowerCase();
                for (String forbidden : FORBIDDEN) {
                    assertThat(name)
                            .as("%s.%s must not reference tournament concept '%s'", type.getSimpleName(), rc.getName(), forbidden)
                            .doesNotContain(forbidden);
                }
            }
        }
    }
}
