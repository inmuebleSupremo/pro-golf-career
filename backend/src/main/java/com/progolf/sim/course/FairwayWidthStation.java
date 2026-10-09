package com.progolf.sim.course;

/** Route-relative V5 fairway dimensions, independently measured golfer-left and golfer-right. */
public record FairwayWidthStation(double routeDistance, double leftHalfWidth, double rightHalfWidth) {
    public FairwayWidthStation {
        if (!Double.isFinite(routeDistance) || routeDistance < 0.0 || !Double.isFinite(leftHalfWidth)
                || !Double.isFinite(rightHalfWidth) || leftHalfWidth < 8.0 || rightHalfWidth < 8.0) {
            throw new IllegalArgumentException("V5 width station must have finite usable dimensions");
        }
    }
}
