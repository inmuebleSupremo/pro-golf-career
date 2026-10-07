package com.progolf.sim.shot;

/** Server-derived coordinate guard, deliberately broader than legal golf terrain. */
public record AimEnvelope(double minX, double maxX, double minY, double maxY) {
    public AimEnvelope {
        if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !Double.isFinite(minY) || !Double.isFinite(maxY)
                || minX > maxX || minY > maxY) throw new IllegalArgumentException("invalid aim envelope");
    }
    public boolean contains(AimPoint point) { return point.x() >= minX && point.x() <= maxX && point.y() >= minY && point.y() <= maxY; }
}
