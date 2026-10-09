package com.progolf.sim.course;

import com.progolf.sim.shot.HoleModel;
import java.util.List;
import java.util.Objects;

/**
 * A complete generated Course: exactly eighteen holes plus identity (REQ-068/069). Total par is derived
 * from the holes and never stored. A Course contains no tournament state (REQ-083). It is reproducible
 * from its seed together with {@code generatorVersion} (REQ-082).
 */
public record Course(CourseIdentity identity, List<GeneratedHole> holes, int generatorVersion,
                     CourseDesignProfile designProfile, CoursePlan coursePlan, CourseArchitecturePlan architecturePlan) {

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

    /** Source-compatible V1/read-model constructor. V1 deliberately has no design profile or plan. */
    public Course(CourseIdentity identity, List<GeneratedHole> holes, int generatorVersion) {
        this(identity, holes, generatorVersion, null, null, null);
    }

    /** Compatibility constructor for retained V2--V4 design-aware course records. */
    public Course(CourseIdentity identity, List<GeneratedHole> holes, int generatorVersion,
                  CourseDesignProfile designProfile, CoursePlan coursePlan) {
        this(identity, holes, generatorVersion, designProfile, coursePlan, null);
    }

    /** Total par, derived from the holes (never stored independently). */
    public int totalPar() {
        int sum = 0;
        for (GeneratedHole h : holes) {
            sum += h.par();
        }
        return sum;
    }

    /** Retains the locked V1--V4 diagnostic rendering while exposing V5 provenance when it exists. */
    @Override
    public String toString() {
        String legacy = "Course[identity=" + identity + ", holes=" + holes + ", generatorVersion=" + generatorVersion
                + ", designProfile=" + designProfile + ", coursePlan=" + coursePlan;
        return architecturePlan == null ? legacy + "]" : legacy + ", architecturePlan=" + architecturePlan + "]";
    }

    /** The playable {@link HoleModel} for a hole number (1..18) in a given round, under the neutral setup. */
    public HoleModel holeModel(int holeNumber, int round) {
        return holeModel(holeNumber, round, CourseSetup.standard());
    }

    /** The playable {@link HoleModel} for a hole (1..18) in a round, under the event's course setup. */
    public HoleModel holeModel(int holeNumber, int round, CourseSetup setup) {
        return holeModel(holeNumber, round, setup, PinPlacementVersion.LEGACY_V1);
    }

    /** The playable model under an explicitly versioned flag-placement policy. */
    public HoleModel holeModel(int holeNumber, int round, CourseSetup setup, PinPlacementVersion pinPlacementVersion) {
        if (holeNumber < 1 || holeNumber > 18) {
            throw new IllegalArgumentException("Hole number must be 1..18: " + holeNumber);
        }
        return holes.get(holeNumber - 1).forRound(round, setup, pinPlacementVersion);
    }
}
