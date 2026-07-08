package com.progolf.sim.course;

/**
 * An active pin position for a hole in a given round (REQ-076). {@code depthOffset} shifts the effective
 * playing length front-to-back (positive = back pin, longer); {@code lateralOffset} is presentation
 * metadata for the pin's side placement. Both are in yards.
 */
public record PinPosition(double depthOffset, double lateralOffset) {

    public PinPosition {
        if (!Double.isFinite(depthOffset) || !Double.isFinite(lateralOffset)) {
            throw new IllegalArgumentException("Pin offsets must be finite");
        }
    }
}
