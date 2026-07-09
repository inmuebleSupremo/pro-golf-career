package com.progolf.sim.media;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** news-generation spec: generators produce real-data-referencing events with prominence (REQ-241/242). */
class NewsFactoryTest {

    @Test
    void victoryReferencesTheWinnerAndEvent() {
        NewsEvent e = NewsFactory.tournamentVictory(3, "g1", "Tiger Woods", "The Open");
        assertThat(e.type()).isEqualTo(NewsType.TOURNAMENT_VICTORY);
        assertThat(e.season()).isEqualTo(3);
        assertThat(e.subjectGolferId()).hasValue("g1");
        assertThat(e.headline()).contains("Tiger Woods").contains("The Open");
        assertThat(e.prominence()).isEqualTo(MediaConstants.PROMINENCE_TOURNAMENT_VICTORY);
    }

    @Test
    void upsetOfAnUnrankedWinnerIsSignificant() {
        NewsEvent e = NewsFactory.majorUpset(5, "g2", "Nobody", "City Classic", Integer.MAX_VALUE);
        assertThat(e.type()).isEqualTo(NewsType.MAJOR_UPSET);
        assertThat(e.headline()).contains("unranked").contains("Nobody");
        assertThat(e.isSignificant()).isTrue();
    }

    @Test
    void worldNumberOneIsTopProminenceAndSignificant() {
        NewsEvent e = NewsFactory.worldNumberOne(4, "g3", "Rory");
        assertThat(e.type()).isEqualTo(NewsType.WORLD_NUMBER_ONE);
        assertThat(e.prominence()).isEqualTo(MediaConstants.PROMINENCE_WORLD_NUMBER_ONE);
        assertThat(e.isSignificant()).isTrue();
    }

    @Test
    void severeWeatherIsWorldLevelWithNoSubject() {
        NewsEvent e = NewsFactory.severeWeather(2, "Storm Open");
        assertThat(e.type()).isEqualTo(NewsType.SEVERE_WEATHER);
        assertThat(e.subjectGolferId()).isEmpty();
        assertThat(e.headline()).contains("Storm Open");
    }

    @Test
    void retirementReportsCareerWins() {
        assertThat(NewsFactory.retirement(9, "g4", "Vet", 1).headline()).contains("1 win");
        assertThat(NewsFactory.retirement(9, "g5", "Champ", 12).headline()).contains("12 wins");
    }
}
