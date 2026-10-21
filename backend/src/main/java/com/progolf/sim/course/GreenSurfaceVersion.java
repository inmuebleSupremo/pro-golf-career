package com.progolf.sim.course;

/** Explicit versions of the reconstructable physical green-surface policy. */
public enum GreenSurfaceVersion {
    /** Initial regular height-field policy; retained by its stable numeric identity. */
    V1(1);

    private final int number;

    GreenSurfaceVersion(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }
}
