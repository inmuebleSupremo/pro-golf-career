package com.progolf.sim.achievement;

/**
 * The fixed catalogue of career achievements a player can unlock (spec: career-achievements). Unlike the
 * old self-chosen career goals, achievements are a curated, game-wide set: every player chases the same list
 * and each one is unlocked once, the first time its condition is met, then remembered for the career.
 *
 * <p>This enum is pure metadata — {@link AchievementCategory}, a display title, a one-line description, and
 * whether it is a {@code secret} (its description is hidden until it is unlocked, a small surprise for the
 * incidental feats). The detection logic lives in {@code AchievementDetector} and the World, never here, so
 * the catalogue can grow without touching how any one is earned. The declared order is the display order
 * within each category.
 */
public enum Achievement {

    // --- Career Milestones (progression landmarks) ---
    PRO_CARD(AchievementCategory.MILESTONE, "Pro Card Picked Up",
            "Qualify for the main professional tour.", false),
    FIRST_SILVERWARE(AchievementCategory.MILESTONE, "First Silverware",
            "Win your first professional tournament.", false),
    MAJOR_MOMENT(AchievementCategory.MILESTONE, "Major Moment",
            "Win your first major championship.", false),
    TOP_OF_THE_WORLD(AchievementCategory.MILESTONE, "Top of the World",
            "Reach the number one spot in the world rankings.", false),
    CAREER_GRAND_SLAM(AchievementCategory.MILESTONE, "The Career Grand Slam",
            "Win all four major championships in a single season.", false),
    HALL_OF_FAMER(AchievementCategory.MILESTONE, "Hall of Famer",
            "Earn induction into the Hall of Fame.", false),

    // --- On-Course Feats (skill & shot-making) ---
    ACE_IN_THE_HOLE(AchievementCategory.ON_COURSE, "Ace in the Hole",
            "Record a hole-in-one during a tournament round.", false),
    ALBATROSS_HUNTER(AchievementCategory.ON_COURSE, "Albatross Hunter",
            "Score three-under par on a single hole (a double eagle).", false),
    FROM_THE_BEACH(AchievementCategory.ON_COURSE, "From the Beach",
            "Hole out directly from a bunker.", false),
    DOWNTOWN_DRAIN(AchievementCategory.ON_COURSE, "Downtown Drain",
            "Sink a putt from fifty feet or more.", false),
    BOGEY_FREE(AchievementCategory.ON_COURSE, "Bogey-Free",
            "Complete a full 18-hole round without a bogey or worse.", false),

    // --- Clutch (drama & pressure) ---
    SUNDAY_CHARGE(AchievementCategory.CLUTCH, "Sunday Charge",
            "Win after trailing by four or more strokes entering the final round.", false),
    ICE_IN_THE_VEINS(AchievementCategory.CLUTCH, "Ice in the Veins",
            "Win a tournament in a sudden-death playoff.", false),
    WIRE_TO_WIRE(AchievementCategory.CLUTCH, "Wire-to-Wire",
            "Lead a tournament after every round, and win.", false),

    // --- Player & Gear Progression (RPG) ---
    FRESH_KICKS_AND_STICKS(AchievementCategory.PROGRESSION, "Fresh Kicks & Sticks",
            "Sign your first sponsorship deal.", false),
    PEAK_CONDITION(AchievementCategory.PROGRESSION, "Peak Condition",
            "Max out every one of your development attributes.", false),

    // --- Quirky & Situational (fun / secret) ---
    THE_SNOWMAN(AchievementCategory.QUIRKY, "The Snowman",
            "Card exactly an eight on a par 3.", true),
    STORM_CHASER(AchievementCategory.QUIRKY, "Storm Chaser",
            "Shoot under par in a round of high wind and heavy rain.", true);

    private final AchievementCategory category;
    private final String title;
    private final String description;
    private final boolean secret;

    Achievement(AchievementCategory category, String title, String description, boolean secret) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.secret = secret;
    }

    public AchievementCategory category() {
        return category;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    /** Whether this achievement's description is hidden until it is unlocked (an incidental surprise feat). */
    public boolean secret() {
        return secret;
    }
}
