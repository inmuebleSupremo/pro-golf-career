package com.progolf.sim.play;

import com.progolf.sim.shot.ShotOutcome;
import java.util.List;
import java.util.Objects;

/**
 * The full shot-by-shot record of one completed hole for the player (spec: career-achievements): the hole
 * number, its par, and the ordered {@link ShotOutcome}s played on it. Retained (unlike the aggregate
 * {@link com.progolf.sim.shot.HoleStats}) so achievement detection can inspect individual shots — a
 * hole-in-one, a bunker hole-out, a long putt drained. Immutable.
 *
 * @param holeNumber the 1-based hole number
 * @param par        the hole's par (3..5)
 * @param shots      the ordered shots played, including the holing stroke
 */
public record PlayedHole(int holeNumber, int par, List<ShotOutcome> shots) {

    public PlayedHole {
        Objects.requireNonNull(shots, "shots");
        shots = List.copyOf(shots);
    }

    /** The total strokes taken on the hole (including any penalties), i.e. the score. */
    public int strokes() {
        int total = 0;
        for (ShotOutcome s : shots) {
            total += s.strokes();
        }
        return total;
    }

    /** Strokes relative to par (negative is under par). */
    public int scoreVsPar() {
        return strokes() - par;
    }
}
