package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.course.CourseSetup;
import org.junit.jupiter.api.Test;

/** course-setup / event-prestige: setup difficulty rises with prestige and with tour-tier field strength. */
class SetupDifficultyTest {

    /** A setup is "harder" when it tucks pins more (higher pinAggression) and plays tighter (lower widthScale). */
    private static void assertHarder(CourseSetup harder, CourseSetup easier) {
        assertThat(harder.pinAggression()).isGreaterThan(easier.pinAggression());
        assertThat(harder.widthScale()).isLessThan(easier.widthScale());
        assertThat(harder.windScale()).isGreaterThanOrEqualTo(easier.windScale());
    }

    @Test
    void prestigeRaisesDifficultyMonotonically() {
        for (Tier tier : Tier.values()) {
            CourseSetup regular = SetupDifficulty.forEvent(tier, EventPrestige.REGULAR);
            CourseSetup signature = SetupDifficulty.forEvent(tier, EventPrestige.SIGNATURE);
            CourseSetup major = SetupDifficulty.forEvent(tier, EventPrestige.MAJOR);
            assertHarder(signature, regular);
            assertHarder(major, signature);
        }
    }

    @Test
    void strongerTierGetsAHarderSetupAtTheSamePrestige() {
        // A stronger-field tour is set up harder so it still scores near even par (field-strength normalization).
        for (EventPrestige prestige : EventPrestige.values()) {
            assertHarder(SetupDifficulty.forEvent(Tier.ELITE, prestige),
                    SetupDifficulty.forEvent(Tier.PREMIER, prestige));
            assertHarder(SetupDifficulty.forEvent(Tier.PREMIER, prestige),
                    SetupDifficulty.forEvent(Tier.STANDARD, prestige));
            assertHarder(SetupDifficulty.forEvent(Tier.STANDARD, prestige),
                    SetupDifficulty.forEvent(Tier.DEVELOPMENT, prestige));
        }
    }

    @Test
    void everyTourPlaysARealGolfCourseAndTheHardestMajorIsTighter() {
        // No tour is set up so wide that the course stops asking questions of a golfer. The entry tour used
        // to play at 1.45x width in pursuit of a "scores near even par" target, which removed the very
        // difficulty that separates a good golfer from a poor one — and handed a strong golfer on a weak tour
        // a -40 week. What separates the tours is the field, not the golf course.
        for (Tier tier : Tier.values()) {
            assertThat(SetupDifficulty.forEvent(tier, EventPrestige.REGULAR).widthScale())
                    .as("%s regular width", tier)
                    .isLessThanOrEqualTo(1.05);
        }
        assertThat(SetupDifficulty.forEvent(Tier.ELITE, EventPrestige.MAJOR).widthScale())
                .isLessThan(1.0);
    }

    @Test
    void theHardestEventStopsShortOfTheLeversLimit() {
        // An Elite major is the hardest thing in the game. If it clamped, it would play identically to an
        // Elite tour championship and to a Premier major, flattening the prestige ladder into a plateau.
        var eliteMajor = SetupDifficulty.forEvent(Tier.ELITE, EventPrestige.MAJOR);
        assertHarder(eliteMajor, SetupDifficulty.forEvent(Tier.ELITE, EventPrestige.TOUR_CHAMPIONSHIP));
        assertHarder(eliteMajor, SetupDifficulty.forEvent(Tier.PREMIER, EventPrestige.MAJOR));
    }
}
