package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import com.progolf.sim.core.Handedness;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolver-owned parametric airborne path. Its endpoint is the authoritative first contact; samples are a
 * presentation projection only and must never become terrain-intersection authority.
 */
public final class FlightSolution {
    private static final int SAMPLE_COUNT = 9;
    private final ShotFrame frame;
    private final double carry;
    private final double lateral;
    private final double curve;
    private final double apex;

    public FlightSolution(ShotFrame frame, double carry, double lateral, double curve, double apex) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.carry = carry;
        this.lateral = lateral;
        this.curve = curve;
        this.apex = Math.max(0.0, apex);
    }

    public Position2d firstContact() { return positionAt(1.0); }
    public Position2d positionAt(double progress) {
        double t = Math.max(0.0, Math.min(1.0, progress));
        return frame.project(carry * t, lateral * t + curve * 4.0 * t * (1.0 - t));
    }
    public double heightAt(double progress) {
        double t = Math.max(0.0, Math.min(1.0, progress));
        return apex * 4.0 * t * (1.0 - t);
    }
    public List<AirbornePoint> samples() {
        List<AirbornePoint> points = new ArrayList<>(SAMPLE_COUNT);
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double t = (double) i / (SAMPLE_COUNT - 1);
            points.add(new AirbornePoint(t, positionAt(t), heightAt(t)));
        }
        return List.copyOf(points);
    }

    /**
     * Signed aim-frame bulge convention. Positive is golfer-right as defined by {@link ShotFrame}; shape names
     * therefore remain meaningful independently of the canonical screen projection.
     */
    static double shapeCurveSign(Handedness handedness, ShotShape shape) {
        double side = handedness == Handedness.LEFT ? -1.0 : 1.0;
        return switch (shape) {
            case DRAW -> side;
            case FADE -> -side;
            case STRAIGHT -> 0.0;
        };
    }
}
