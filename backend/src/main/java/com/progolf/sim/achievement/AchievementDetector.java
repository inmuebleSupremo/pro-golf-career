package com.progolf.sim.achievement;

import com.progolf.sim.play.PlayedHole;
import com.progolf.sim.play.PlayerRoundRecord;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.SimConstants;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.weather.PlayingConditions;
import java.util.Collection;
import java.util.List;

/**
 * Detects the play-based {@link Achievement}s from a player's captured golf (spec: career-achievements). A
 * pure, stateless function of the shot-by-shot record — it never touches world or career state, so it is
 * fully unit-testable, and it computes no shot outcomes (it only reads already-resolved ones).
 *
 * <p>The metric- and milestone-based achievements (pro card, first win, world number one, Hall of Fame,
 * peak condition, first sponsorship, career grand slam) are evaluated by the World against live career
 * state and are not detected here — this class covers only what can be read off the shots and standings of
 * an event: the on-course feats, the round feats, and the two standings-based clutch feats.
 */
public final class AchievementDetector {

    /** A putt of this length or more (feet) drained is a "Downtown Drain". */
    static final double LONG_PUTT_FEET = 50.0;
    /** Wind at or above this (mph) counts as the "high wind" half of a storm round. */
    static final double STORM_WIND_MPH = 18.0;
    /** Rain at or above this (normalized [0,1]) counts as the "heavy rain" half of a storm round. */
    static final double STORM_RAIN = 0.50;
    /** Strokes behind entering the final round at or above which a comeback win is a "Sunday Charge". */
    static final int SUNDAY_CHARGE_DEFICIT = 4;

    private AchievementDetector() {
    }

    /**
     * Adds any on-course feats earned on one completed hole: a hole-in-one, a double eagle, a bunker
     * hole-out, a fifty-foot putt, or an eight on a par 3.
     */
    public static void detectHoleFeats(PlayedHole hole, Collection<Achievement> out) {
        List<ShotOutcome> shots = hole.shots();
        if (shots.isEmpty()) {
            return;
        }

        // Ace: the tee shot was the only stroke and it holed out.
        if (shots.size() == 1 && holed(shots.get(0))) {
            out.add(Achievement.ACE_IN_THE_HOLE);
        }
        // Albatross / double eagle: three under par on the hole.
        if (hole.scoreVsPar() == -3) {
            out.add(Achievement.ALBATROSS_HUNTER);
        }
        // The Snowman: exactly an eight on a par 3.
        if (hole.par() == 3 && hole.strokes() == 8) {
            out.add(Achievement.THE_SNOWMAN);
        }
        // Feats defined by the shot the ball was HOLED with, read against the lie it was played from
        // (the previous shot's finishing surface — a non-hazard surface, so this is exact).
        for (int k = 1; k < shots.size(); k++) {
            ShotOutcome holingShot = shots.get(k);
            if (!holed(holingShot)) {
                continue;
            }
            ShotOutcome previous = shots.get(k - 1);
            if (previous.finalSurface() == Surface.BUNKER) {
                out.add(Achievement.FROM_THE_BEACH);
            }
            if (previous.finalSurface() == Surface.GREEN
                    && previous.distanceRemaining() * SimConstants.YARDS_TO_FEET >= LONG_PUTT_FEET) {
                out.add(Achievement.DOWNTOWN_DRAIN);
            }
        }
    }

    /** Adds any round feats earned over one completed 18-hole round: a bogey-free card, or a storm round. */
    public static void detectRoundFeats(PlayerRoundRecord round, Collection<Achievement> out) {
        List<PlayedHole> holes = round.holes();
        if (holes.size() == 18 && bogeyFree(holes)) {
            out.add(Achievement.BOGEY_FREE);
        }
        if (round.scoreVsPar() < 0 && storm(round.conditions())) {
            out.add(Achievement.STORM_CHASER);
        }
    }

    /**
     * Adds the standings-based clutch feats from a completed event, given whether the player won it: a
     * come-from-behind win (Sunday Charge) and leading start to finish (Wire-to-Wire). Both require the win.
     */
    public static void detectEventFeats(List<PlayerRoundRecord> rounds, boolean playerWon,
                                        Collection<Achievement> out) {
        if (!playerWon) {
            return;
        }
        // Wire-to-wire: a full four rounds, leading (or tied for the lead) after every one.
        if (rounds.size() == 4 && rounds.stream().allMatch(PlayerRoundRecord::led)) {
            out.add(Achievement.WIRE_TO_WIRE);
        }
        // Sunday charge: trailing by four or more entering the final round, then winning.
        for (PlayerRoundRecord r : rounds) {
            if (r.roundNumber() == 3 && r.strokesBehindAfter() >= SUNDAY_CHARGE_DEFICIT) {
                out.add(Achievement.SUNDAY_CHARGE);
            }
        }
    }

    /** Whether a shot holed out (finished in the cup, not via a penalty hazard). */
    private static boolean holed(ShotOutcome shot) {
        return !shot.hazardEntered() && shot.distanceRemaining() <= SimConstants.HOLED_THRESHOLD;
    }

    private static boolean bogeyFree(List<PlayedHole> holes) {
        for (PlayedHole h : holes) {
            if (h.scoreVsPar() > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean storm(PlayingConditions conditions) {
        return conditions.windSpeed() >= STORM_WIND_MPH && conditions.rain() >= STORM_RAIN;
    }
}
