package com.progolf.sim.shot;

/**
 * The environmental conditions relevant to resolving a single shot (REQ-057). Positive
 * {@code headWind} blows into the shot (reducing reach); negative is a tailwind. {@code crossWind} is
 * unsigned magnitude affecting lateral dispersion. {@code lieQuality} is in [0,1] where 1.0 is a
 * perfect lie.
 */
public record Environment(WindVector wind, double lieQuality) {

    public Environment {
        if (wind == null || !Double.isFinite(lieQuality)) {
            throw new IllegalArgumentException("Environment values must be finite");
        }
        if (lieQuality < 0 || lieQuality > 1) {
            throw new IllegalArgumentException("lieQuality must be in [0,1]: " + lieQuality);
        }
    }

    /** Legacy local-frame input: positive headwind and unsigned crosswind, retained for fixtures. */
    public Environment(double headWind, double crossWind, double lieQuality) {
        this(new WindVector(crossWind, -headWind), lieQuality);
    }

    /** Legacy local-frame views used only by non-spatial callers. */
    public double headWind() { return -wind.y(); }
    public double crossWind() { return Math.abs(wind.x()); }

    /** Calm conditions with a perfect lie. */
    public static Environment calm() {
        return new Environment(new WindVector(0.0, 0.0), 1.0);
    }
}
