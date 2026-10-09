package com.progolf.sim.course;

import java.util.Objects;

/** Deterministic environmental intent for one V5 hole, compiled only through canonical terrain. */
record EnvironmentHoleCharacter(TreeEnclosure treeEnclosure, boolean coastalExposure) {
    EnvironmentHoleCharacter {
        Objects.requireNonNull(treeEnclosure, "treeEnclosure");
    }
}
