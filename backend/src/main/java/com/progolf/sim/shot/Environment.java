package com.progolf.sim.shot;

/**
 * The environmental conditions relevant to resolving a single shot (REQ-057). Positive
 * {@code headWind} blows into the shot (reducing reach); negative is a tailwind. {@code crossWind} is
 * unsigned magnitude affecting lateral dispersion. {@code lieQuality} and {@code groundFirmness} are in
 * [0,1], where 1.0 respectively represents a perfect lie and firmest ground.
 */
public record Environment(WindVector wind, double lieQuality, double groundFirmness) {

    public Environment {
        if (wind == null || !Double.isFinite(lieQuality) || !Double.isFinite(groundFirmness)) {
            throw new IllegalArgumentException("Environment values must be finite");
        }
        if (lieQuality < 0 || lieQuality > 1) {
            throw new IllegalArgumentException("lieQuality must be in [0,1]: " + lieQuality);
        }
        if (groundFirmness < 0 || groundFirmness > 1) {
            throw new IllegalArgumentException("groundFirmness must be in [0,1]: " + groundFirmness);
        }
    }

    /** Compatibility constructor for callers that predate resolver-facing ground firmness. */
    public Environment(WindVector wind, double lieQuality) {
        this(wind, lieQuality, 0.65);
    }

    /** Legacy local-frame input: positive headwind and unsigned crosswind, retained for fixtures. */
    public Environment(double headWind, double crossWind, double lieQuality) {
        this(new WindVector(crossWind, -headWind), lieQuality, 0.65);
    }

    /** Legacy local-frame views used only by non-spatial callers. */
    public double headWind() { return -wind.y(); }
    public double crossWind() { return Math.abs(wind.x()); }

    /** Calm conditions with a perfect lie. */
    public static Environment calm() {
        return new Environment(new WindVector(0.0, 0.0), 1.0, 1.0);
    }
}
