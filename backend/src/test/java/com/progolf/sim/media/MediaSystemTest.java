package com.progolf.sim.media;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** news-generation / world-narrative spec: feed accumulation, significance discoverability, narrative. */
class MediaSystemTest {

    @Test
    void publishAppendsAndSignificantNewsIsDiscoverable() {
        MediaSystem media = new MediaSystem();
        media.publish(NewsFactory.tournamentVictory(1, "g1", "A", "Open"));   // prominence 60, not significant
        media.publish(NewsFactory.worldNumberOne(1, "g2", "B"));              // prominence 95, significant

        assertThat(media.size()).isEqualTo(2);
        assertThat(media.feed()).hasSize(2);
        assertThat(media.significantNews()).singleElement()
                .satisfies(e -> assertThat(e.type()).isEqualTo(NewsType.WORLD_NUMBER_ONE));
    }

    @Test
    void newsForGolferFiltersBySubject() {
        MediaSystem media = new MediaSystem();
        media.publish(NewsFactory.tournamentVictory(1, "g1", "A", "Open"));
        media.publish(NewsFactory.tournamentVictory(1, "g2", "B", "Classic"));
        media.publish(NewsFactory.severeWeather(1, "Storm Open")); // no subject

        assertThat(media.newsForGolfer("g1")).singleElement()
                .satisfies(e -> assertThat(e.subjectGolferId()).hasValue("g1"));
    }

    @Test
    void lastWinTrackingDrivesDroughtAndRecency() {
        MediaSystem media = new MediaSystem();
        media.publish(NewsFactory.tournamentVictory(2, "g1", "A", "Open")); // last win in season 2

        // Five seasons later with an old win and few titles ⇒ a drought.
        assertThat(media.careerNarrative("g1", 33, 8, 3, 20, 7)).isEqualTo(CareerNarrative.CHAMPIONSHIP_DROUGHT);
        // The same season as the win ⇒ not a drought (a dominant, top-ranked, many-time winner here).
        assertThat(media.careerNarrative("g1", 30, 12, 15, 1, 2)).isEqualTo(CareerNarrative.DOMINANT_CHAMPION);
        // A golfer with no recorded win is treated as never having won.
        assertThat(media.careerNarrative("g9", 20, 2, 0, 40, 5)).isEqualTo(CareerNarrative.RISING_PROSPECT);
    }
}
