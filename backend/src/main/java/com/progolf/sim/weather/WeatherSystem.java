package com.progolf.sim.weather;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.course.EnvironmentClassification;
import java.util.ArrayList;
import java.util.List;

/**
 * The World's Weather System (spec: weather-generation, REQ-228): it deterministically generates a
 * {@link TournamentWeather} for a scheduled event from the world seed hierarchy, the course's
 * environmental classification, and the point in the season. World-owned and independent of any golfer;
 * it produces environmental state and nothing else (REQ-239).
 *
 * <p>Determinism: the per-tournament seed derives from {@code (masterSeed, season, tournament)} plus a
 * dedicated {@link WeatherConstants#WEATHER_SALT}, isolating the weather stream from the shot streams
 * (which chain through round/golfer/hole/shot), so weather never perturbs play and the world stays
 * reproducible (REQ-232/299).
 */
public final class WeatherSystem {

    private final long masterSeed;

    public WeatherSystem(long masterSeed) {
        this.masterSeed = masterSeed;
    }

    /** Generates the defined per-round conditions and forecast for a tournament. */
    public TournamentWeather generate(long seasonId, long tournamentId, double seasonPhase,
                                      EnvironmentClassification classification, int rounds) {
        if (rounds < 1) {
            throw new IllegalArgumentException("rounds must be >= 1: " + rounds);
        }
        long seed = Seeds.deriveSeed(
                Seeds.deriveSeed(Seeds.deriveSeed(masterSeed, seasonId), tournamentId),
                WeatherConstants.WEATHER_SALT);
        Rng rng = new SplitMix64Rng(seed);

        List<PlayingConditions> perRound = new ArrayList<>(rounds);
        PlayingConditions current = ClimateModel.baseConditions(classification, seasonPhase, rng);
        perRound.add(current);
        for (int r = 2; r <= rounds; r++) {
            current = ClimateModel.evolve(current, rng);
            perRound.add(current);
        }
        Forecast forecast = Forecast.of(perRound.get(0), rng);
        return new TournamentWeather(perRound, forecast);
    }
}
