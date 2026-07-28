package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;
import org.junit.jupiter.api.Test;

/**
 * Scene-backdrop authoring (spec: play-event imagery): a curated Pro event's scene comes from its authored
 * identity, not the host course's (randomly assigned) classification — so its place, name, and imagery always
 * agree. The host {@code course} passed here is deliberately DESERT to prove it does not leak into curated
 * events; it is only consulted for the procedural Development tour.
 */
class EventNamingCourseTypeTest {

    private static final Course DESERT_COURSE =
            CourseGenerator.generate(new SeedCoordinate(1L, 1, 1, 0, 0, 0, 0), EnvironmentClassification.DESERT);

    private static String proMajor(int majorOrdinal) {
        return EventNaming.courseType(EventPrestige.MAJOR, TourTier.PRO, DESERT_COURSE, majorOrdinal, -1);
    }

    private static String proEvent(int proEventOrdinal) {
        return EventNaming.courseType(EventPrestige.REGULAR, TourTier.PRO, DESERT_COURSE, -1, proEventOrdinal);
    }

    @Test
    void curatedMajorsCarryTheirAuthoredScene() {
        // The Continental Championship (Kentucky, 4th major) is parkland — never desert, whatever the host course.
        assertThat(proMajor(3)).isEqualTo("PARKLAND");
        // The Seaside Open (Scotland, 3rd major) is the links test.
        assertThat(proMajor(2)).isEqualTo("LINKS");
    }

    @Test
    void curatedProEventsCarryTheirAuthoredScene() {
        assertThat(proEvent(2)).isEqualTo("TROPICAL");  // Sunshine State Showdown (Florida)
        assertThat(proEvent(13)).isEqualTo("MOUNTAIN"); // Blue Ridge Mountain Open
        assertThat(proEvent(14)).isEqualTo("DESERT");   // Desert Mountain Challenge (authored, coincidentally desert)
        assertThat(proEvent(1)).isEqualTo("COASTAL");   // Coastal Heritage Classic
    }

    @Test
    void developmentTourDerivesSceneFromClassification() {
        // Off the curated tour (proEventOrdinal -1), the host course's environment drives the scene.
        assertThat(EventNaming.courseType(EventPrestige.REGULAR, TourTier.DEVELOPMENT, DESERT_COURSE, -1, -1))
                .isEqualTo("DESERT");
    }
}
