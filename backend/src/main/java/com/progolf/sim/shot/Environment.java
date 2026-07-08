package com.progolf.sim.shot;

/**
 * The environmental conditions relevant to resolving a single shot (REQ-057). Positive
 * {@code headWind} blows into the shot (reducing reach); negative is a tailwind. {@code crossWind} is
 * unsigned magnitude affecting lateral dispersion. {@code lieQuality} is in [0,1] where 1.0 is a
 * perfect lie.
 */
public record Environment(double headWind, double crossWind, double lieQuality) {

    public Environment {
        if (!Double.isFinite(headWind) || !Double.isFinite(crossWind) || !Double.isFinite(lieQuality)) {
            throw new IllegalArgumentException("Environment values must be finite");
        }
        if (lieQuality < 0 || lieQuality > 1) {
            throw new IllegalArgumentException("lieQuality must be in [0,1]: " + lieQuality);
        }
    }

    /** Calm conditions with a perfect lie. */
    public static Environment calm() {
        return new Environment(0.0, 0.0, 1.0);
    }
}
