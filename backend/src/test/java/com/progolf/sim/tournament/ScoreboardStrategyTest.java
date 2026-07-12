package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.shot.Strategy;
import org.junit.jupiter.api.Test;

/** tournament-play: the scoreboard bends a golfer's strategy on the closing rounds. */
class ScoreboardStrategyTest {

    private static final int CLOSING = TournamentConstants.SCOREBOARD_CLOSING_ROUND;
    private static final int PRESS = TournamentConstants.SCOREBOARD_PRESS_BEHIND;
    private static final int PROTECT = TournamentConstants.SCOREBOARD_PROTECT_MARGIN;

    @Test
    void openingRoundsKeepTheDisposition() {
        for (Strategy d : Strategy.values()) {
            // Far behind or big lead — it does not matter before the closing rounds.
            assertThat(ScoreboardStrategy.adjust(d, CLOSING - 1, PRESS + 5, 0)).isEqualTo(d);
            assertThat(ScoreboardStrategy.adjust(d, CLOSING - 1, 0, PROTECT + 5)).isEqualTo(d);
        }
    }

    @Test
    void aChaserPressesOnTheClosingRounds() {
        // A conservative or balanced golfer well behind presses on the closing round.
        assertThat(ScoreboardStrategy.adjust(Strategy.CONSERVATIVE, CLOSING, PRESS, 0)).isEqualTo(Strategy.AGGRESSIVE);
        assertThat(ScoreboardStrategy.adjust(Strategy.BALANCED, CLOSING, PRESS + 10, 0)).isEqualTo(Strategy.AGGRESSIVE);
    }

    @Test
    void aComfortableLeaderProtects() {
        // A leader (0 behind) with a big margin over the field protects.
        assertThat(ScoreboardStrategy.adjust(Strategy.AGGRESSIVE, CLOSING, 0, PROTECT)).isEqualTo(Strategy.CONSERVATIVE);
        assertThat(ScoreboardStrategy.adjust(Strategy.BALANCED, CLOSING, 0, PROTECT + 3)).isEqualTo(Strategy.CONSERVATIVE);
    }

    @Test
    void inThePackTheDispositionStands() {
        for (Strategy d : Strategy.values()) {
            assertThat(ScoreboardStrategy.adjust(d, CLOSING, PRESS - 1, 0)).isEqualTo(d);   // just in contention
            assertThat(ScoreboardStrategy.adjust(d, CLOSING, 0, PROTECT - 1)).isEqualTo(d); // leading only narrowly
        }
    }
}
