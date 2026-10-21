package com.progolf.sim.course;

import java.util.Objects;

/** Elevation and local gradient from the authoritative green-local height field. */
public record GreenSurfaceSample(double elevation, GreenSurfaceGradient gradient, GreenSurfaceLocation location) {
    public GreenSurfaceSample {
        if (!Double.isFinite(elevation)) {
            throw new IllegalArgumentException("green-surface elevation must be finite");
        }
        Objects.requireNonNull(gradient, "gradient");
        Objects.requireNonNull(location, "location");
    }
}
