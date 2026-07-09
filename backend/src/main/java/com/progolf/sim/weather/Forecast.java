package com.progolf.sim.weather;

import com.progolf.sim.core.Rng;

/**
 * A pre-tournament forecast of expected {@link PlayingConditions} (spec: tournament-weather, REQ-238). It
 * is a deterministic bounded perturbation of the opening conditions — it approximates them to support
 * strategic preparation, but is a <em>prediction</em>, never a guarantee, so it does not equal the
 * conditions actually experienced.
 */
public record Forecast(PlayingConditions predicted) {

    public Forecast {
        if (predicted == null) {
            throw new IllegalArgumentException("predicted must not be null");
        }
    }

    /** A forecast derived from the actual opening conditions plus bounded deterministic error. */
    static Forecast of(PlayingConditions actual, Rng rng) {
        double windSpeed = actual.windSpeed() + rng.nextGaussian() * WeatherConstants.FORECAST_WIND_ERROR;
        double windDirection = actual.windDirection() + rng.nextGaussian() * WeatherConstants.FORECAST_DIR_ERROR;
        double rain = actual.rain() + rng.nextGaussian() * WeatherConstants.FORECAST_RAIN_ERROR;
        double temperature = actual.temperature() + rng.nextGaussian() * WeatherConstants.FORECAST_TEMP_ERROR;
        double humidity = actual.humidity() + rng.nextGaussian() * WeatherConstants.FORECAST_HUMIDITY_ERROR;
        return new Forecast(PlayingConditions.of(windSpeed, windDirection, rain, temperature, humidity));
    }
}
