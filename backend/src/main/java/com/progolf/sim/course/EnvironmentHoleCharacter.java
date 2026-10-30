package com.progolf.sim.course;

import java.util.Objects;

/** Deterministic environmental intent for one V5 hole, compiled only through canonical terrain. */
record EnvironmentHoleCharacter(TreeEnclosure treeEnclosure, boolean coastalExposure, boolean oceanCoast) {
    EnvironmentHoleCharacter(TreeEnclosure treeEnclosure, boolean coastalExposure) {
        this(treeEnclosure, coastalExposure, false);
    }
    EnvironmentHoleCharacter {
        Objects.requireNonNull(treeEnclosure, "treeEnclosure");
    }
}

