package com.progolf.sim.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.course.EnvironmentClassification;
import org.junit.jupiter.api.Test;

/** weather-generation spec: deterministic generation, stream isolation, climate variety, forecast, severity. */
class WeatherGenerationTest {

    private static final long SEED = 0xB0A7L;

    @Test
    void generationIsDeterministicForTheSameCoordinates() {
        WeatherSystem sys = new WeatherSystem(SEED);
        TournamentWeather a = sys.generate(3, 7, 0.5, EnvironmentClassification.LINKS, 4);
        TournamentWeather b = sys.generate(3, 7, 0.5, EnvironmentClassification.LINKS, 4);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentTournamentsGetIndependentWeather() {
        WeatherSystem sys = new WeatherSystem(SEED);
        TournamentWeather t1 = sys.generate(3, 1, 0.5, EnvironmentClassification.LINKS, 4);
        TournamentWeather t2 = sys.generate(3, 2, 0.5, EnvironmentClassification.LINKS, 4);
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void sameMasterSeedReproducesAcrossSystems() {
        TournamentWeather a = new WeatherSystem(SEED).generate(1, 1, 0.2, EnvironmentClassification.DESERT, 4);
        TournamentWeather b = new WeatherSystem(SEED).generate(1, 1, 0.2, EnvironmentClassification.DESERT, 4);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void perRoundConditionsAreProducedAndPlayoffUsesTheFinalRound() {
        TournamentWeather tw = new WeatherSystem(SEED).generate(1, 1, 0.5, EnvironmentClassification.COASTAL, 4);
        assertThat(tw.rounds()).hasSize(4);
        // Round numbers beyond the defined rounds (playoff holes) reuse the final round's conditions.
        assertThat(tw.conditionsForRound(92)).isEqualTo(tw.conditionsForRound(4));
        // Shared within a round: the same instance is returned for every competitor.
        assertThat(tw.conditionsForRound(2)).isSameAs(tw.conditionsForRound(2));
    }

    @Test
    void exposedClassificationsTendWindierThanShelteredOnes() {
        WeatherSystem sys = new WeatherSystem(SEED);
        double links = 0;
        double woodland = 0;
        int n = 60;
        for (int t = 1; t <= n; t++) {
            links += sys.generate(1, t, 0.5, EnvironmentClassification.LINKS, 4).conditionsForRound(1).windSpeed();
            woodland += sys.generate(1, t, 0.5, EnvironmentClassification.WOODLAND, 4).conditionsForRound(1).windSpeed();
        }
        assertThat(links / n).isGreaterThan(woodland / n);
    }

    @Test
    void midSeasonRunsWarmerThanTheSeasonEdges() {
        WeatherSystem sys = new WeatherSystem(SEED);
        double mid = 0;
        double edge = 0;
        int n = 60;
        for (int t = 1; t <= n; t++) {
            mid += sys.generate(1, t, 0.5, EnvironmentClassification.PARKLAND, 4).conditionsForRound(1).temperature();
            edge += sys.generate(1, t, 0.0, EnvironmentClassification.PARKLAND, 4).conditionsForRound(1).temperature();
        }
        assertThat(mid / n).isGreaterThan(edge / n);
    }

    @Test
    void forecastApproximatesButDoesNotGuaranteeTheConditions() {
        TournamentWeather tw = new WeatherSystem(SEED).generate(4, 9, 0.4, EnvironmentClassification.LINKS, 4);
        PlayingConditions actual = tw.conditionsForRound(1);
        PlayingConditions predicted = tw.forecast().predicted();
        // A prediction, not a guarantee: close to, but not equal to, the actual opening conditions.
        assertThat(predicted).isNotEqualTo(actual);
        assertThat(Math.abs(predicted.windSpeed() - actual.windSpeed())).isLessThan(20.0);
    }

    @Test
    void severeConditionsDoOccurOnExposedStormyCourses() {
        WeatherSystem sys = new WeatherSystem(SEED);
        long severe = 0;
        for (int t = 1; t <= 60; t++) {
            // Exposed links at a stormy season edge — record-worthy weather should sometimes appear.
            double sev = sys.generate(1, t, 0.0, EnvironmentClassification.LINKS, 4).severity();
            if (sev >= WeatherConstants.SEVERITY_THRESHOLD) {
                severe++;
            }
        }
        assertThat(severe).isGreaterThan(0);
    }
}
