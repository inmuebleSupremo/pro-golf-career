package com.progolf.sim.shot;

/** World-space wind flow in yards, decomposed against each actual shot axis by the resolver. */
public record WindVector(double x, double y) {
    public WindVector {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("wind must be finite");
    }
    public double against(ShotFrame frame) { return -(x * frame.forwardX() + y * frame.forwardY()); }
    public double rightward(ShotFrame frame) { return x * frame.forwardY() - y * frame.forwardX(); }
}
