package com.progolf.sim.course;

/** Immutable rigid transform between one hole's local yards and shared course-yard coordinates. */
public record CourseCoordinateTransform(Position2d origin, double headingRadians) {
    public CourseCoordinateTransform {
        if (origin == null || !Double.isFinite(headingRadians)) {
            throw new IllegalArgumentException("course transform requires a finite origin and heading");
        }
    }

    public Position2d toCourse(Position2d local) {
        double cos = StrictMath.cos(headingRadians);
        double sin = StrictMath.sin(headingRadians);
        return new Position2d(origin.x() + local.x() * cos + local.y() * sin,
                origin.y() - local.x() * sin + local.y() * cos);
    }

    public Position2d toLocal(Position2d course) {
        double dx = course.x() - origin.x();
        double dy = course.y() - origin.y();
        double cos = StrictMath.cos(headingRadians);
        double sin = StrictMath.sin(headingRadians);
        return new Position2d(dx * cos - dy * sin, dx * sin + dy * cos);
    }
}
