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
    void theEasiestTourIsWiderThanNeutralAndTheHardestMajorTighter() {
        // The weakest tour's regular events play wider than baseline; the strongest tour's major plays tighter.
        assertThat(SetupDifficulty.forEvent(Tier.DEVELOPMENT, EventPrestige.REGULAR).widthScale())
                .isGreaterThan(1.0);
        assertThat(SetupDifficulty.forEvent(Tier.ELITE, EventPrestige.MAJOR).widthScale())
                .isLessThan(1.0);
    }
}
