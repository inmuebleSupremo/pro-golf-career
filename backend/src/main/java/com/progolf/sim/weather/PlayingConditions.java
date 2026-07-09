package com.progolf.sim.weather;

import com.progolf.sim.shot.Environment;

/**
 * The environmental state experienced during play (spec: playing-conditions, REQ-229). Immutable and
 * internally consistent: {@code groundFirmness}, {@code greenSpeed}, and {@code visibility} are always
 * <em>derived</em> from the primary drivers (rain, humidity) via {@link #of}, so a state can never be
 * incoherent (torrential rain on rock-firm, lightning-fast greens). Conditions describe the environment;
 * they perform no shot calculation and modify nothing (REQ-230/236/237/239).
 *
 * <p>Normalized fields ({@code rain}, {@code humidity}, {@code groundFirmness}, {@code greenSpeed},
 * {@code visibility}) are in [0,1]; {@code windSpeed} is a non-negative speed and {@code windDirection}
 * is the bearing (degrees, [0,360)) the wind blows <em>from</em>; {@code temperature} is unconstrained.
 */
public record PlayingConditions(
        double windSpeed,
        double windDirection,
        double rain,
        double temperature,
        double humidity,
        double groundFirmness,
        double greenSpeed,
        double visibility) {

    public PlayingConditions {
        requireFinite(windSpeed, "windSpeed");
        requireFinite(windDirection, "windDirection");
        requireFinite(rain, "rain");
        requireFinite(temperature, "temperature");
        requireFinite(humidity, "humidity");
        requireFinite(groundFirmness, "groundFirmness");
        requireFinite(greenSpeed, "greenSpeed");
        requireFinite(visibility, "visibility");
        if (windSpeed < 0) {
            throw new IllegalArgumentException("windSpeed must be >= 0: " + windSpeed);
        }
        if (windDirection < 0 || windDirection >= 360) {
            throw new IllegalArgumentException("windDirection must be in [0,360): " + windDirection);
        }
        requireUnit(rain, "rain");
        requireUnit(humidity, "humidity");
        requireUnit(groundFirmness, "groundFirmness");
        requireUnit(greenSpeed, "greenSpeed");
        requireUnit(visibility, "visibility");
    }

    /**
     * Builds internally consistent conditions from the primary drivers, deriving the coupled surface
     * fields (REQ-229/230/237): rain softens the ground, slows the greens, and lowers visibility; dry air
     * firms the ground; firmer ground runs faster greens.
     */
    public static PlayingConditions of(double windSpeed, double windDirection, double rain,
                                       double temperature, double humidity) {
        double dir = wrap360(windDirection);
        double r = clamp01(rain);
        double h = clamp01(humidity);
        double firmness = clamp01(WeatherConstants.BASE_FIRMNESS
                - r * WeatherConstants.RAIN_FIRMNESS
                + (1.0 - h) * WeatherConstants.DRY_FIRMNESS_BONUS);
        double greenSpeed = clamp01(WeatherConstants.BASE_GREEN_SPEED
                - r * WeatherConstants.RAIN_GREEN_SLOW
                + (firmness - WeatherConstants.BASE_FIRMNESS) * WeatherConstants.FIRM_GREEN_FAST);
        double visibility = clamp01(1.0 - r * WeatherConstants.RAIN_VISIBILITY);
        return new PlayingConditions(Math.max(0, windSpeed), dir, r, temperature, h,
                firmness, greenSpeed, visibility);
    }

    /**
     * Calm, dry, perfectly firm conditions. Chosen so {@link #environmentForHole} maps exactly to
     * {@link Environment#calm()} (no wind, perfect lie), letting a weather-free tournament reproduce prior
     * behaviour bit-for-bit.
     */
    public static PlayingConditions calm() {
        return new PlayingConditions(0.0, 0.0, 0.0, WeatherConstants.BASE_TEMP, WeatherConstants.BASE_HUMIDITY,
                1.0, WeatherConstants.BASE_GREEN_SPEED, 1.0);
    }

    /**
     * Translates these conditions into the shared shot {@link Environment} for a given hole (REQ-233):
     * wind is scaled by the course's exposure and decomposed against a per-hole play direction into a
     * signed {@code headWind} (positive into the shot) and an unsigned {@code crossWind}; {@code lieQuality}
     * follows ground firmness and rain. This is a pure translation into the shot engine's input contract —
     * it performs no shot mathematics (REQ-239).
     */
    public Environment environmentForHole(int holeNumber, double courseExposure) {
        double bearing = (holeNumber * WeatherConstants.BEARING_STEP_DEGREES) % 360.0;
        double theta = StrictMath.toRadians(windDirection - bearing);
        double effective = windSpeed * WeatherConstants.WIND_TO_YARDS * courseExposure;
        // `+ 0.0` normalizes a possible -0.0 (0 wind times a negative cosine) so calm maps exactly to
        // Environment.calm(); it leaves every other value unchanged.
        double headWind = effective * StrictMath.cos(theta) + 0.0;
        double crossWind = Math.abs(effective * StrictMath.sin(theta)) * WeatherConstants.CROSSWIND_SCALE;
        double lieQuality = clamp(
                1.0 - rain * WeatherConstants.RAIN_LIE_PENALTY
                        - (1.0 - groundFirmness) * WeatherConstants.SOFT_LIE_PENALTY,
                WeatherConstants.LIE_FLOOR, 1.0);
        return new Environment(headWind, crossWind, lieQuality);
    }

    /**
     * A [0,1] measure of how punishing these conditions are — driven by wind, rain, and lost visibility.
     * Used to flag significant environmental history (REQ-235).
     */
    public double severity() {
        double windSev = clamp01(windSpeed / WeatherConstants.SEVERITY_WIND_REF);
        return clamp01(windSev * WeatherConstants.SEV_WIND_WEIGHT
                + rain * WeatherConstants.SEV_RAIN_WEIGHT
                + (1.0 - visibility) * WeatherConstants.SEV_VISIBILITY_WEIGHT);
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0, 1.0);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static double wrap360(double deg) {
        double d = deg % 360.0;
        return d < 0 ? d + 360.0 : d;
    }

    private static void requireFinite(double v, String field) {
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException(field + " must be finite: " + v);
        }
    }

    private static void requireUnit(double v, String field) {
        if (v < 0 || v > 1) {
            throw new IllegalArgumentException(field + " must be in [0,1]: " + v);
        }
    }
}
