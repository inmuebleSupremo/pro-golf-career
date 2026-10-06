package com.progolf.sim.course;

import java.util.Objects;

/** Compact permanent venue intent used only by the V2 generator. */
public record CourseDesignProfile(StrategicEmphasis strategicEmphasis, WidthTendency widthTendency,
                                  RecoverySeverity recoverySeverity) {
    public CourseDesignProfile {
        Objects.requireNonNull(strategicEmphasis, "strategicEmphasis");
        Objects.requireNonNull(widthTendency, "widthTendency");
        Objects.requireNonNull(recoverySeverity, "recoverySeverity");
    }
}
