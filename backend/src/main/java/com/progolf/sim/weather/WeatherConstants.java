package com.progolf.sim.weather;

/**
 * The single tunables surface for the Weather domain (spec: weather-generation / playing-conditions /
 * tournament-weather). All magnitudes live here so climate character and severity can be calibrated in
 * one place without touching logic. Units are internal and consistent: wind in mph, temperature in
 * degrees Fahrenheit, normalized fields (rain, humidity, firmness, green speed, visibility) in [0,1].
 */
public final class WeatherConstants {

    private WeatherConstants() {
    }

    /**
     * Distinct salt mixed into the per-tournament weather seed so the weather random stream is isolated
     * from every shot/round/golfer stream (which chain through round/golfer/hole/shot coordinates). A
     * large prime unlikely to collide with any round identifier (rounds are 1–4, playoffs 91+).
     */
    public static final long WEATHER_SALT = 999_999_937L;

    // --- Climate base means (opening conditions) ---
    public static final double BASE_WIND = 4.0;            // mph floor before exposure
    public static final double WIND_EXPOSURE_SPAN = 14.0;  // added at full course exposure
    public static final double WIND_SEASONAL = 4.0;        // extra wind at stormy season edges
    public static final double WIND_NOISE = 2.5;           // gaussian sd on opening wind
    public static final double RAIN_MEAN = 0.15;
    public static final double RAIN_SEASONAL = 0.15;       // extra rain at stormy season edges
    public static final double RAIN_NOISE = 0.12;
    public static final double BASE_TEMP = 60.0;
    public static final double TEMP_SEASONAL_AMP = 18.0;   // warmer mid-season
    public static final double TEMP_NOISE = 4.0;
    public static final double BASE_HUMIDITY = 0.45;
    public static final double RAIN_HUMIDITY = 0.40;       // rain raises humidity
    public static final double HUMIDITY_NOISE = 0.08;

    // --- Round-to-round evolution volatility (bounded random walk) ---
    public static final double WIND_VOLATILITY = 3.0;
    public static final double DIR_VOLATILITY = 25.0;      // degrees
    public static final double RAIN_VOLATILITY = 0.12;
    public static final double TEMP_VOLATILITY = 4.0;
    public static final double HUMIDITY_VOLATILITY = 0.08;

    // --- Coupling: derived firmness / green speed / visibility (internal consistency) ---
    public static final double BASE_FIRMNESS = 0.65;
    public static final double RAIN_FIRMNESS = 0.55;       // rain softens ground
    public static final double DRY_FIRMNESS_BONUS = 0.20;  // dry air firms ground
    public static final double BASE_GREEN_SPEED = 0.60;
    public static final double RAIN_GREEN_SLOW = 0.45;     // rain slows greens
    public static final double FIRM_GREEN_FAST = 0.60;     // firmer greens run faster
    public static final double RAIN_VISIBILITY = 0.55;     // rain lowers visibility

    // --- Mapping to the shot Environment ---
    public static final double WIND_TO_YARDS = 0.85;       // mph -> effective carry yards
    public static final double CROSSWIND_SCALE = 0.55;     // dampen crosswind vs headwind
    public static final double BEARING_STEP_DEGREES = 47.0; // per-hole play-direction spread
    public static final double RAIN_LIE_PENALTY = 0.15;    // wet lies
    public static final double SOFT_LIE_PENALTY = 0.10;    // soft/plugged lies
    public static final double LIE_FLOOR = 0.60;

    // --- Severity (significant environmental history) ---
    public static final double SEVERITY_WIND_REF = 25.0;   // mph mapping to full wind severity
    public static final double SEV_WIND_WEIGHT = 0.55;
    public static final double SEV_RAIN_WEIGHT = 0.30;
    public static final double SEV_VISIBILITY_WEIGHT = 0.15;
    public static final double SEVERITY_THRESHOLD = 0.60;  // record conditions at/above this

    // --- Forecast error (prediction, not guarantee) ---
    public static final double FORECAST_WIND_ERROR = 3.0;
    public static final double FORECAST_DIR_ERROR = 20.0;
    public static final double FORECAST_RAIN_ERROR = 0.10;
    public static final double FORECAST_TEMP_ERROR = 4.0;
    public static final double FORECAST_HUMIDITY_ERROR = 0.08;
}
