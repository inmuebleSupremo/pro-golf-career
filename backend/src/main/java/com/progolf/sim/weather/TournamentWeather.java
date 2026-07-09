package com.progolf.sim.weather;

import java.util.List;

/**
 * The weather for one Tournament (spec: tournament-weather, REQ-231/232): a defined sequence of
 * {@link PlayingConditions}, one per round, plus a pre-tournament {@link Forecast}. Conditions are
 * determined before play and may evolve round to round, but within a round every competitor shares the
 * same conditions ({@link #conditionsForRound}), so play is field-fair. Immutable.
 */
public record TournamentWeather(List<PlayingConditions> rounds, Forecast forecast) {

    public TournamentWeather {
        if (rounds == null || rounds.isEmpty()) {
            throw new IllegalArgumentException("rounds must be non-empty");
        }
        if (forecast == null) {
            throw new IllegalArgumentException("forecast must not be null");
        }
        rounds = List.copyOf(rounds);
    }

    /**
     * The shared conditions for a round (1-based). Round numbers beyond the defined rounds — playoff
     * holes in particular — use the final round's conditions.
     */
    public PlayingConditions conditionsForRound(int roundNo) {
        int index = roundNo - 1;
        if (index < 0) {
            index = 0;
        } else if (index >= rounds.size()) {
            index = rounds.size() - 1;
        }
        return rounds.get(index);
    }

    /** The peak severity across the tournament's rounds (REQ-235). */
    public double severity() {
        double max = 0.0;
        for (PlayingConditions pc : rounds) {
            max = Math.max(max, pc.severity());
        }
        return max;
    }

    /** Calm weather for every round — the default for a tournament played without supplied weather. */
    public static TournamentWeather calm() {
        PlayingConditions calm = PlayingConditions.calm();
        return new TournamentWeather(List.of(calm), new Forecast(calm));
    }
}
