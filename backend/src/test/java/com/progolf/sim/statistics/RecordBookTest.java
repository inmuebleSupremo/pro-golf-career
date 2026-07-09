package com.progolf.sim.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** records-archive spec: records surpass in their direction and preserve progression (REQ-254/255). */
class RecordBookTest {

    @Test
    void higherIsBetterRecordSurpassesAndPreservesTheProgression() {
        RecordBook book = new RecordBook();
        assertThat(book.current(RecordType.MOST_CAREER_WINS)).isEmpty();

        assertThat(book.challenge(RecordType.MOST_CAREER_WINS, "g1", 3, 1)).isTrue();
        assertThat(book.challenge(RecordType.MOST_CAREER_WINS, "g2", 2, 2)).isFalse(); // 2 does not beat 3
        assertThat(book.challenge(RecordType.MOST_CAREER_WINS, "g2", 5, 3)).isTrue();  // 5 beats 3

        assertThat(book.current(RecordType.MOST_CAREER_WINS)).hasValueSatisfying(h -> {
            assertThat(h.golferId()).isEqualTo("g2");
            assertThat(h.value()).isEqualTo(5);
            assertThat(h.season()).isEqualTo(3);
        });
        // The previous holder is preserved as progression (REQ-255).
        assertThat(book.progression(RecordType.MOST_CAREER_WINS))
                .extracting(RecordHolder::golferId).containsExactly("g1", "g2");
    }

    @Test
    void lowerIsBetterForTournamentScore() {
        RecordBook book = new RecordBook();
        assertThat(book.challenge(RecordType.LOWEST_TOURNAMENT_SCORE, "g1", -5, 1)).isTrue();
        assertThat(book.challenge(RecordType.LOWEST_TOURNAMENT_SCORE, "g2", -3, 2)).isFalse(); // -3 worse than -5
        assertThat(book.challenge(RecordType.LOWEST_TOURNAMENT_SCORE, "g2", -8, 3)).isTrue();  // -8 better

        assertThat(book.current(RecordType.LOWEST_TOURNAMENT_SCORE).orElseThrow().value()).isEqualTo(-8);
        assertThat(book.progression(RecordType.LOWEST_TOURNAMENT_SCORE)).hasSize(2);
    }
}
