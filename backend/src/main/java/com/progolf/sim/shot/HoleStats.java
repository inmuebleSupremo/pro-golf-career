package com.progolf.sim.shot;

import com.progolf.sim.spatial.Surface;
import java.util.List;

/**
 * Shot-level statistics derived from the shots played on one hole (spec: competitive-statistics): whether a
 * fairway was in play and hit (drives, i.e. par 4/5 only), whether the green was reached in regulation
 * (on the green or holed within {@code par − 2} strokes), and the number of putts (strokes played from the
 * green). A pure function of the already-resolved shots and the hole's par; it computes no shot outcomes.
 */
public record HoleStats(boolean fairwayEligible, boolean fairwayHit, boolean greenInRegulation, int putts) {

    /** Derives the hole's shot stats from its ordered shot outcomes and its par. */
    public static HoleStats of(List<ShotOutcome> shots, int par) {
        boolean fairwayEligible = par >= 4; // a par 3 has no drive
        boolean fairwayHit = fairwayEligible && !shots.isEmpty()
                && shots.get(0).finalSurface() == Surface.FAIRWAY;

        // Green in regulation: the ball reaches the green (or holes out) using at most par-2 strokes.
        int strokesToGreen = 0;
        boolean reachedGreen = false;
        for (ShotOutcome s : shots) {
            strokesToGreen += s.strokes();
            boolean holed = !s.hazardEntered() && s.distanceRemaining() <= SimConstants.HOLED_THRESHOLD;
            if (s.finalSurface() == Surface.GREEN || holed) {
                reachedGreen = true;
                break;
            }
        }
        boolean greenInRegulation = reachedGreen && strokesToGreen <= par - 2;

        // Putts: strokes played from the green (a shot whose starting lie — the previous shot's final
        // surface — is the green). The approach that lands on the green is not itself a putt.
        int putts = 0;
        for (int j = 1; j < shots.size(); j++) {
            if (shots.get(j - 1).finalSurface() == Surface.GREEN) {
                putts++;
            }
        }
        return new HoleStats(fairwayEligible, fairwayHit, greenInRegulation, putts);
    }
}
