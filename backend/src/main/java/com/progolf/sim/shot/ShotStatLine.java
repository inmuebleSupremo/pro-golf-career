package com.progolf.sim.shot;

/**
 * An accumulated shot-statistics line (spec: competitive-statistics): fairways hit and possible, greens in
 * regulation, holes played, and putts. Immutable; {@link #plus} folds a hole or another line, so the same
 * type serves per-round, per-event, and career aggregation. Exposes the standard rates.
 */
public record ShotStatLine(int fairwaysHit, int fairwaysPossible, int greensInRegulation, int holesPlayed,
                           int putts) {

    /** An empty line (the accumulation identity). */
    public static ShotStatLine empty() {
        return new ShotStatLine(0, 0, 0, 0, 0);
    }

    /** Folds one hole's stats into this line. */
    public ShotStatLine plus(HoleStats h) {
        return new ShotStatLine(
                fairwaysHit + (h.fairwayHit() ? 1 : 0),
                fairwaysPossible + (h.fairwayEligible() ? 1 : 0),
                greensInRegulation + (h.greenInRegulation() ? 1 : 0),
                holesPlayed + 1,
                putts + h.putts());
    }

    /** Folds another line into this one. */
    public ShotStatLine plus(ShotStatLine o) {
        return new ShotStatLine(
                fairwaysHit + o.fairwaysHit,
                fairwaysPossible + o.fairwaysPossible,
                greensInRegulation + o.greensInRegulation,
                holesPlayed + o.holesPlayed,
                putts + o.putts);
    }

    /** Fraction of eligible drives that found the fairway (0 when no par-4/5 holes played). */
    public double drivingAccuracy() {
        return fairwaysPossible == 0 ? 0.0 : (double) fairwaysHit / fairwaysPossible;
    }

    /** Fraction of holes hit in regulation (0 when none played). */
    public double greensInRegulationRate() {
        return holesPlayed == 0 ? 0.0 : (double) greensInRegulation / holesPlayed;
    }

    /** Putts per 18-hole round (0 when none played). */
    public double puttsPerRound() {
        return holesPlayed == 0 ? 0.0 : putts * 18.0 / holesPlayed;
    }
}
