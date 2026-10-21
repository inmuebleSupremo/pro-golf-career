package com.progolf.sim.putt;

import com.progolf.sim.course.GreenSurfaceVersion;

/**
 * Event-pinned putting-resolution policy. Legacy remains the default for missing historical policy data;
 * physical putting is deliberately opt-in and pins both component versions.
 */
public enum PuttingPolicy {
    LEGACY(null, null),
    PHYSICAL_V1(PhysicalPuttModelVersion.V1, GreenSurfaceVersion.V1);

    private final PhysicalPuttModelVersion physicalModelVersion;
    private final GreenSurfaceVersion surfaceVersion;

    PuttingPolicy(PhysicalPuttModelVersion physicalModelVersion, GreenSurfaceVersion surfaceVersion) {
        this.physicalModelVersion = physicalModelVersion;
        this.surfaceVersion = surfaceVersion;
    }

    public boolean usesPhysicalPutting() {
        return physicalModelVersion != null;
    }

    public PhysicalPuttModelVersion physicalModelVersion() {
        return physicalModelVersion;
    }

    public GreenSurfaceVersion surfaceVersion() {
        return surfaceVersion;
    }
}
