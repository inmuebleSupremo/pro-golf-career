package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/**
 * Deterministic local coordinate frame for projecting a sampled shot into canonical yard-space.
 *
 * <p>The legacy sampler measures carry down a green-centre reference line and lateral displacement
 * across that line. The active pin is a signed lateral target within that frame; it must not also
 * rotate the frame. Each full-shot frame starts at the actual playable ball position, so a miss on
 * one shot is not re-applied as an offset on the next one.
 */
public final class ShotFrame {

    private final Position2d origin;
    private final double forwardX;
    private final double forwardY;

    private ShotFrame(Position2d origin, double forwardX, double forwardY) {
        this.origin = origin;
        this.forwardX = forwardX;
        this.forwardY = forwardY;
    }

    /**
     * Builds the full-shot frame used by the legacy carry/lateral model.
     *
     * <p>The reference preserves the cup's front/back position but places it on the green centre
     * line. Consequently, {@code targetLateral} remains the sole lateral offset that aims at a
     * tucked pin. This is valid for the V1 canonical frame, whose green centre line is parallel to
     * its local y axis; the transient course centreline is intentionally not persisted.
     */
    public static ShotFrame towardGreenCentreReference(Position2d origin, Position2d greenCenter, Position2d cup) {
        Objects.requireNonNull(greenCenter, "greenCenter");
        Objects.requireNonNull(cup, "cup");
        return toward(origin, new Position2d(greenCenter.x(), cup.y()));
    }

    /**
     * Chooses the legacy-compatible full-shot frame, falling back to the physical cup when the
     * centreline reference is behind the ball (for example, after an over-green miss).
     */
    public static ShotFrame forFullShot(Position2d origin, Position2d greenCenter, Position2d cup) {
        ShotFrame centreline = towardGreenCentreReference(origin, greenCenter, cup);
        return centreline.forwardTo(cup) > 0.0 ? centreline : toward(origin, cup);
    }

    /** Builds a direct frame for motion whose distance is already measured to the physical cup (putts). */
    public static ShotFrame toward(Position2d origin, Position2d target) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(target, "target");
        double dx = target.x() - origin.x();
        double dy = target.y() - origin.y();
        double length = StrictMath.hypot(dx, dy);
        if (length == 0.0) {
            return new ShotFrame(origin, 0.0, 1.0);
        }
        return new ShotFrame(origin, dx / length, dy / length);
    }

    /** Maps legacy local {@code (forward, lateral)} coordinates to canonical world coordinates. */
    public Position2d project(double forward, double lateral) {
        if (!Double.isFinite(forward) || !Double.isFinite(lateral)) {
            throw new IllegalArgumentException("shot coordinates must be finite");
        }
        // Positive lateral is golfer-right: clockwise from the forward axis.
        return origin.plus(forwardX * forward + forwardY * lateral,
                forwardY * forward - forwardX * lateral);
    }

    /** Forward coordinate of a world position in this frame. */
    public double forwardTo(Position2d position) {
        Objects.requireNonNull(position, "position");
        return (position.x() - origin.x()) * forwardX + (position.y() - origin.y()) * forwardY;
    }

    /** Signed golfer-right lateral coordinate of a world position in this frame. */
    public double lateralTo(Position2d position) {
        Objects.requireNonNull(position, "position");
        return (position.x() - origin.x()) * forwardY - (position.y() - origin.y()) * forwardX;
    }
    public double forwardX() { return forwardX; }
    public double forwardY() { return forwardY; }
}
