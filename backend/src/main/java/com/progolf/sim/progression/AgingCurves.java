package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;

/**
 * Attribute-specific aging (REQ-159/160). The per-season change for an attribute is the difference of its
 * class's rounded cumulative offset between this age and the previous — small, integer, and gradual, and
 * non-uniform across the physical/skill/mental classes.
 */
public final class AgingCurves {

    private AgingCurves() {
    }

    /** The signed per-season aging change for an attribute at a given age. */
    public static int seasonDelta(Attribute attribute, int age) {
        AgingClass cls = AgingClass.of(attribute);
        long now = Math.round(cls.offsetAt(age));
        long prev = Math.round(cls.offsetAt(age - 1));
        return (int) (now - prev);
    }
}
