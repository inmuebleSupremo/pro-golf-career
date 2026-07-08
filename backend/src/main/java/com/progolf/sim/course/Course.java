package com.progolf.sim.course;

import com.progolf.sim.shot.HoleModel;
import java.util.List;
import java.util.Objects;

/**
 * A complete generated Course: exactly eighteen holes plus identity (REQ-068/069). Total par is derived
 * from the holes and never stored. A Course contains no tournament state (REQ-083). It is reproducible
 * from its seed together with {@code generatorVersion} (REQ-082).
 */
public record Course(CourseIdentity identity, List<GeneratedHole> holes, int generatorVersion) {

    public Course {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(holes, "holes");
        holes = List.copyOf(holes);
        if (holes.size() != 18) {
            throw new IllegalArgumentException("A Course must have exactly 18 holes, got " + holes.size());
        }
        for (int i = 0; i < 18; i++) {
            if (holes.get(i).number() != i + 1) {
                throw new IllegalArgumentException("Holes must be numbered 1..18 in order; index " + i
                        + " has number " + holes.get(i).number());
            }
        }
    }

    /** Total par, derived from the holes (never stored independently). */
    public int totalPar() {
        int sum = 0;
        for (GeneratedHole h : holes) {
            sum += h.par();
        }
        return sum;
    }

    /** The playable {@link HoleModel} for a hole number (1..18) in a given round, with that round's pin. */
    public HoleModel holeModel(int holeNumber, int round) {
        if (holeNumber < 1 || holeNumber > 18) {
            throw new IllegalArgumentException("Hole number must be 1..18: " + holeNumber);
        }
        return holes.get(holeNumber - 1).forRound(round);
    }
}
