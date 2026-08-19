package com.progolf.sim.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.play.PlayedHole;
import com.progolf.sim.play.PlayerRoundRecord;
import com.progolf.sim.shot.FactorBreakdown;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.weather.PlayingConditions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** career-achievements: the pure detector reads feats off a player's shot-by-shot record and standings. */
class AchievementDetectorTest {

    private static final FactorBreakdown F = new FactorBreakdown(0, 0, 0, 0);

    /** A resolved shot finishing on {@code surface} with {@code distRem} yards left (no penalty). */
    private static ShotOutcome shot(Surface surface, double distRem) {
        return new ShotOutcome(surface, 0.0, 0.0, distRem, false, 0, 1, F);
    }

    /** A holing stroke (in the cup). */
    private static ShotOutcome holed() {
        return shot(Surface.GREEN, 0.0);
    }

    private static Set<Achievement> holeFeats(PlayedHole hole) {
        Set<Achievement> out = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectHoleFeats(hole, out);
        return out;
    }

    @Test
    void aceInTheHole() {
        PlayedHole hole = new PlayedHole(7, 3, List.of(holed()));
        assertThat(holeFeats(hole)).contains(Achievement.ACE_IN_THE_HOLE);
    }

    @Test
    void albatrossOnAParFive() {
        // Two strokes on a par 5 is three under: a double eagle.
        PlayedHole hole = new PlayedHole(15, 5, List.of(shot(Surface.FAIRWAY, 240), holed()));
        assertThat(holeFeats(hole))
                .contains(Achievement.ALBATROSS_HUNTER)
                .doesNotContain(Achievement.ACE_IN_THE_HOLE);
    }

    @Test
    void holeOutFromABunker() {
        PlayedHole hole = new PlayedHole(4, 4,
                List.of(shot(Surface.FAIRWAY, 150), shot(Surface.BUNKER, 18), holed()));
        assertThat(holeFeats(hole)).contains(Achievement.FROM_THE_BEACH);
    }

    @Test
    void longPuttDrained() {
        // A putt from 20 yards (60 ft) drained — over the fifty-foot bar.
        PlayedHole hole = new PlayedHole(9, 4,
                List.of(shot(Surface.FAIRWAY, 150), shot(Surface.GREEN, 20.0), holed()));
        assertThat(holeFeats(hole)).contains(Achievement.DOWNTOWN_DRAIN);
    }

    @Test
    void shortPuttIsNotADrain() {
        PlayedHole hole = new PlayedHole(9, 4,
                List.of(shot(Surface.FAIRWAY, 150), shot(Surface.GREEN, 3.0), holed()));
        assertThat(holeFeats(hole)).doesNotContain(Achievement.DOWNTOWN_DRAIN);
    }

    @Test
    void snowmanOnAParThree() {
        List<ShotOutcome> shots = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            shots.add(shot(Surface.FAIRWAY, 40)); // seven scrambles
        }
        shots.add(holed()); // the eighth stroke drops
        PlayedHole hole = new PlayedHole(12, 3, shots);
        assertThat(hole.strokes()).isEqualTo(8);
        assertThat(holeFeats(hole)).contains(Achievement.THE_SNOWMAN);
    }

    @Test
    void bogeyFreeRound() {
        List<PlayedHole> holes = new ArrayList<>();
        for (int h = 1; h <= 18; h++) {
            holes.add(new PlayedHole(h, 4, List.of(shot(Surface.GREEN, 6), holed()))); // birdie every hole
        }
        Set<Achievement> out = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectRoundFeats(record(1, holes, PlayingConditions.calm(), 1, 0), out);
        assertThat(out).contains(Achievement.BOGEY_FREE);
    }

    @Test
    void oneBogeyBreaksTheStreak() {
        List<PlayedHole> holes = new ArrayList<>();
        for (int h = 1; h <= 17; h++) {
            holes.add(new PlayedHole(h, 4, List.of(shot(Surface.GREEN, 6), holed())));
        }
        // A bogey (5 on a par 4).
        holes.add(new PlayedHole(18, 4, List.of(shot(Surface.FAIRWAY, 150), shot(Surface.FAIRWAY, 60),
                shot(Surface.GREEN, 6), shot(Surface.GREEN, 2), holed())));
        Set<Achievement> out = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectRoundFeats(record(1, holes, PlayingConditions.calm(), 1, 0), out);
        assertThat(out).doesNotContain(Achievement.BOGEY_FREE);
    }

    @Test
    void stormChaserUnderParInWindAndRain() {
        List<PlayedHole> holes = List.of(new PlayedHole(1, 4, List.of(shot(Surface.GREEN, 6), holed())));
        PlayingConditions storm = PlayingConditions.of(22.0, 90.0, 0.7, 55.0, 0.9);
        Set<Achievement> out = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectRoundFeats(record(2, holes, storm, 1, 0), out);
        assertThat(out).contains(Achievement.STORM_CHASER);
    }

    @Test
    void calmUnderParIsNotAStorm() {
        List<PlayedHole> holes = List.of(new PlayedHole(1, 4, List.of(shot(Surface.GREEN, 6), holed())));
        Set<Achievement> out = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectRoundFeats(record(2, holes, PlayingConditions.calm(), 1, 0), out);
        assertThat(out).doesNotContain(Achievement.STORM_CHASER);
    }

    @Test
    void wireToWireRequiresLeadingEveryRoundAndWinning() {
        List<PlayerRoundRecord> led = List.of(
                record(1, List.of(), PlayingConditions.calm(), 1, 0),
                record(2, List.of(), PlayingConditions.calm(), 1, 0),
                record(3, List.of(), PlayingConditions.calm(), 1, 0),
                record(4, List.of(), PlayingConditions.calm(), 1, 0));
        Set<Achievement> won = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectEventFeats(led, true, won);
        assertThat(won).contains(Achievement.WIRE_TO_WIRE);

        Set<Achievement> lost = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectEventFeats(led, false, lost);
        assertThat(lost).doesNotContain(Achievement.WIRE_TO_WIRE);
    }

    @Test
    void trailingAfterARoundBreaksWireToWire() {
        List<PlayerRoundRecord> rounds = List.of(
                record(1, List.of(), PlayingConditions.calm(), 1, 0),
                record(2, List.of(), PlayingConditions.calm(), 2, 1), // slipped to second
                record(3, List.of(), PlayingConditions.calm(), 1, 0),
                record(4, List.of(), PlayingConditions.calm(), 1, 0));
        Set<Achievement> out = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectEventFeats(rounds, true, out);
        assertThat(out).doesNotContain(Achievement.WIRE_TO_WIRE);
    }

    @Test
    void sundayChargeFromFourBack() {
        List<PlayerRoundRecord> rounds = List.of(
                record(1, List.of(), PlayingConditions.calm(), 8, 6),
                record(2, List.of(), PlayingConditions.calm(), 6, 5),
                record(3, List.of(), PlayingConditions.calm(), 3, 5), // five back entering the final round
                record(4, List.of(), PlayingConditions.calm(), 1, 0));
        Set<Achievement> won = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectEventFeats(rounds, true, won);
        assertThat(won).contains(Achievement.SUNDAY_CHARGE);
    }

    @Test
    void noChargeWhenCloseEnteringTheFinalRound() {
        List<PlayerRoundRecord> rounds = List.of(
                record(3, List.of(), PlayingConditions.calm(), 2, 2), // only two back
                record(4, List.of(), PlayingConditions.calm(), 1, 0));
        Set<Achievement> out = EnumSet.noneOf(Achievement.class);
        AchievementDetector.detectEventFeats(rounds, true, out);
        assertThat(out).doesNotContain(Achievement.SUNDAY_CHARGE);
    }

    private static PlayerRoundRecord record(int roundNo, List<PlayedHole> holes, PlayingConditions conditions,
                                            int position, int behind) {
        return new PlayerRoundRecord(roundNo, holes, conditions, position, behind);
    }
}
