package com.progolf.sim.putt;

/** Version boundary for opt-in physical putting resolution. */
public enum PhysicalPuttModelVersion {
    V1(1);

    private final int number;

    PhysicalPuttModelVersion(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }
}
