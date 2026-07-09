package com.progolf.sim.weather;

import com.progolf.sim.core.Rng;
import com.progolf.sim.course.EnvironmentClassification;

/**
 * The seasonal + course-classification climate model (spec: weather-generation, REQ-234). It seeds a
 * tournament's opening {@link PlayingConditions} from the course's environmental exposure and the point
 * in the season, and evolves conditions round to round as a bounded random walk. Pure and deterministic:
 * every draw comes from the supplied {@link Rng}; transcendental math uses {@link StrictMath}.
 *
 * <p>Climate patterns stay internally consistent — more exposed courses tend windier, and the season
 * edges are cooler and stormier than mid-season — so weather contributes to course identity and World
 * variety without ever being fabricated to raise difficulty (REQ-237).
 */
final class ClimateModel {

    private ClimateModel() {
    }

    /** Opening conditions for a tournament: driven by course exposure and season phase (0=start, 1=end). */
    static PlayingConditions baseConditions(EnvironmentClassification classification, double seasonPhase, Rng rng) {
        double phase = clamp01(seasonPhase);
        double warmth = StrictMath.sin(StrictMath.PI * phase); // 0 at edges, 1 mid-season
        double storminess = 1.0 - warmth;                      // stormier at the season edges

        double windSpeed = WeatherConstants.BASE_WIND
                + classification.exposure() * WeatherConstants.WIND_EXPOSURE_SPAN
                + storminess * WeatherConstants.WIND_SEASONAL
                + rng.nextGaussian() * WeatherConstants.WIND_NOISE;
        double windDirection = rng.nextDouble() * 360.0;
        double rain = WeatherConstants.RAIN_MEAN
                + storminess * WeatherConstants.RAIN_SEASONAL
                + rng.nextGaussian() * WeatherConstants.RAIN_NOISE;
        double temperature = WeatherConstants.BASE_TEMP
                + warmth * WeatherConstants.TEMP_SEASONAL_AMP
                + rng.nextGaussian() * WeatherConstants.TEMP_NOISE;
        double humidity = WeatherConstants.BASE_HUMIDITY
                + rain * WeatherConstants.RAIN_HUMIDITY
                + rng.nextGaussian() * WeatherConstants.HUMIDITY_NOISE;

        return PlayingConditions.of(windSpeed, windDirection, rain, temperature, humidity);
    }

    /** Evolves conditions into the next round via a bounded random walk from the primary drivers. */
    static PlayingConditions evolve(PlayingConditions prev, Rng rng) {
        double windSpeed = prev.windSpeed() + rng.nextGaussian() * WeatherConstants.WIND_VOLATILITY;
        double windDirection = prev.windDirection() + rng.nextGaussian() * WeatherConstants.DIR_VOLATILITY;
        double rain = prev.rain() + rng.nextGaussian() * WeatherConstants.RAIN_VOLATILITY;
        double temperature = prev.temperature() + rng.nextGaussian() * WeatherConstants.TEMP_VOLATILITY;
        double humidity = prev.humidity() + rng.nextGaussian() * WeatherConstants.HUMIDITY_VOLATILITY;
        return PlayingConditions.of(windSpeed, windDirection, rain, temperature, humidity);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1.0);
    }
}
