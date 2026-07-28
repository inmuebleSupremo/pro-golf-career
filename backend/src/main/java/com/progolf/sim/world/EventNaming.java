package com.progolf.sim.world;

import com.progolf.sim.course.Course;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;

/**
 * Deterministic display names + locations for scheduled events (spec: world-schedule).
 *
 * <p>The <b>Pro tour</b> — where a player spends most of their career — is a <b>fixed, curated calendar</b>:
 * the four majors, the season-ending Tour Championship, and {@link #PRO_EVENTS} (the non-major, non-final
 * events) all carry stable names and locations, identical every season and every game, so the top tour has a
 * consistent narrative. The names are original but aligned in placement/flavour to real tour stops (a
 * non-licensable nod — no real event names are used). The <b>Development tour</b> stays procedural: its
 * signature/regular events take their host venue's generated name, so the feeder tour varies year to year.
 *
 * <p>Pure and stable — the same event names the same way wherever it appears (upcoming schedule or a
 * completed result).
 */
final class EventNaming {

    private EventNaming() {
    }

    /**
     * The visual character of a course, used only to pick the play/hub scene backdrop (spec: play-event
     * imagery). A superset of {@link EnvironmentClassification} — it adds {@code TROPICAL} (a Florida-style
     * feel that the sim has no separate classification for) and folds WOODLAND into PARKLAND for imagery.
     * Authored per curated Pro event below; derived from the host course's classification for the (procedural)
     * Development tour, whose region now matches its classification.
     */
    enum Scene {
        PARKLAND, LINKS, DESERT, TROPICAL, MOUNTAIN, COASTAL
    }

    /** Fixed fictional major names, in season order — evocative of the four real majors without copying them. */
    static final String[] MAJOR_NAMES = {
            "The Grandmaster Invitational", // early-season invitational
            "The National Open",            // the national championship
            "The Seaside Open",             // a links/coastal test
            "The Continental Championship", // the late-season championship
    };

    /**
     * Fixed major locations, parallel to {@link #MAJOR_NAMES} — a non-licensable place-name hint toward the
     * real major each evokes (Georgia → the Masters, Scotland → the Open, etc.). Editable here to taste.
     */
    static final String[] MAJOR_LOCATIONS = {
            "Georgia",   // The Grandmaster Invitational → the Masters
            "New York",  // The National Open → the U.S. Open
            "Scotland",  // The Seaside Open → the Open Championship
            "Kentucky",  // The Continental Championship → the PGA Championship
    };

    /** Scene backdrops for the majors, parallel to {@link #MAJOR_LOCATIONS} — the seaside major is a links, the rest parkland. */
    static final Scene[] MAJOR_SCENES = {
            Scene.PARKLAND, // Georgia (Augusta-style parkland)
            Scene.PARKLAND, // New York
            Scene.LINKS,    // Scotland (the seaside/links test)
            Scene.PARKLAND, // Kentucky
    };

    /** The Pro tour's season-ending Tour Championship location — fixed (a nod to East Lake, Atlanta). */
    static final String PRO_CHAMPIONSHIP_LOCATION = "Georgia";

    /** The Pro Tour Championship's scene — parkland (East Lake, Atlanta). */
    static final Scene PRO_CHAMPIONSHIP_SCENE = Scene.PARKLAND;

    /** One fixed Pro-tour event: a stable name, location, and scene so the top tour reads the same every season. */
    record ProEvent(String name, String location, Scene scene) {
    }

    /**
     * The Pro tour's fixed non-major, non-championship events, in calendar order — assigned to the tour's
     * non-anchor slots by ascending week, so early-season stops land early and late stops land late. Original
     * names/places aligned in flavour to real tour stops (comment names the inspiration; no real name is used).
     *
     * <p>At the standard density there are exactly as many Pro non-anchor slots as entries here (currently 15);
     * if the density constant ({@code SeasonCadence.TARGET_EVENTS_PER_TIER}) changes, the assignment wraps
     * (modulo) — add or trim entries to keep a clean one-to-one. Edit freely.
     */
    static final ProEvent[] PRO_EVENTS = {
            new ProEvent("Lone Star Invitational", "Texas", Scene.PARKLAND),               // ~ Valero Texas Open
            new ProEvent("Coastal Heritage Classic", "South Carolina", Scene.COASTAL),     // ~ RBC Heritage
            new ProEvent("Sunshine State Showdown", "Florida", Scene.TROPICAL),            // ~ Florida spring swing
            new ProEvent("Queen City Championship", "North Carolina", Scene.PARKLAND),     // ~ Truist Championship (Quail Hollow)
            new ProEvent("Irish Links Open", "Ireland", Scene.LINKS),                      // ~ Irish Open
            new ProEvent("Great Lakes Classic", "Michigan", Scene.PARKLAND),               // ~ Detroit-area events
            new ProEvent("Canadian National Open", "Ontario", Scene.PARKLAND),             // ~ RBC Canadian Open
            new ProEvent("New England Invitational", "Connecticut", Scene.PARKLAND),       // ~ Travelers Championship
            new ProEvent("Prairie State Open", "Illinois", Scene.PARKLAND),                // ~ John Deere Classic
            new ProEvent("Links of Britain Championship", "Scotland", Scene.LINKS),        // ~ Scottish Open / Open window
            new ProEvent("Twin Cities Classic", "Minnesota", Scene.PARKLAND),              // ~ 3M Open
            new ProEvent("Old North State Championship", "North Carolina", Scene.PARKLAND), // ~ Wyndham Championship
            new ProEvent("Mississippi Valley Invitational", "Tennessee", Scene.PARKLAND),  // ~ FedEx St. Jude (Memphis)
            new ProEvent("Blue Ridge Mountain Open", "North Carolina", Scene.MOUNTAIN),    // ~ Asheville-area event
            new ProEvent("Desert Mountain Challenge", "Utah", Scene.DESERT),               // ~ Bank of Utah Championship
    };

    /**
     * The display name for an event. {@code majorOrdinal} is its 0-based place among the season's majors (-1
     * if not a major); {@code proEventOrdinal} is its 0-based place among the Pro tour's non-major,
     * non-championship events (-1 otherwise) — the index into {@link #PRO_EVENTS}.
     */
    static String name(EventPrestige prestige, TourTier tier, Course course, int majorOrdinal, int proEventOrdinal) {
        return switch (prestige) {
            case MAJOR -> MAJOR_NAMES[Math.floorMod(majorOrdinal, MAJOR_NAMES.length)];
            case TOUR_CHAMPIONSHIP -> tierLabel(tier) + " Tour Championship";
            case SIGNATURE, REGULAR -> proEventOrdinal >= 0
                    ? PRO_EVENTS[Math.floorMod(proEventOrdinal, PRO_EVENTS.length)].name()
                    : "The " + course.identity().name() + (prestige == EventPrestige.SIGNATURE ? " Invitational" : " Open");
        };
    }

    /**
     * The display location for an event: a major carries its fixed hinting place; a Pro non-anchor event its
     * fixed curated location; the Pro Tour Championship its fixed finale location; everything else (the whole
     * Development tour) is placed at its host course, so its location is that venue's region.
     */
    static String location(EventPrestige prestige, TourTier tier, Course course, int majorOrdinal,
                           int proEventOrdinal) {
        return switch (prestige) {
            case MAJOR -> MAJOR_LOCATIONS[Math.floorMod(majorOrdinal, MAJOR_LOCATIONS.length)];
            case TOUR_CHAMPIONSHIP -> tier == TourTier.PRO ? PRO_CHAMPIONSHIP_LOCATION : course.identity().region();
            case SIGNATURE, REGULAR -> proEventOrdinal >= 0
                    ? PRO_EVENTS[Math.floorMod(proEventOrdinal, PRO_EVENTS.length)].location()
                    : course.identity().region();
        };
    }

    /**
     * The scene backdrop token for an event (spec: play-event imagery). Curated Pro events carry an authored
     * scene so their place, name, and imagery always agree (a Scotland major is a links, a Florida stop is
     * tropical) regardless of the host course's generated classification; the Development tour derives the
     * scene from its host course's classification, which its region already matches. Returned as the enum
     * name, e.g. {@code "LINKS"}.
     */
    static String courseType(EventPrestige prestige, TourTier tier, Course course, int majorOrdinal,
                             int proEventOrdinal) {
        return switch (prestige) {
            case MAJOR -> MAJOR_SCENES[Math.floorMod(majorOrdinal, MAJOR_SCENES.length)].name();
            case TOUR_CHAMPIONSHIP -> tier == TourTier.PRO
                    ? PRO_CHAMPIONSHIP_SCENE.name()
                    : sceneFor(course).name();
            case SIGNATURE, REGULAR -> proEventOrdinal >= 0
                    ? PRO_EVENTS[Math.floorMod(proEventOrdinal, PRO_EVENTS.length)].scene().name()
                    : sceneFor(course).name();
        };
    }

    /** The scene for a procedurally-generated course, from its environment (WOODLAND reads as parkland imagery). */
    private static Scene sceneFor(Course course) {
        EnvironmentClassification classification = course.identity().classification();
        return switch (classification) {
            case LINKS -> Scene.LINKS;
            case DESERT -> Scene.DESERT;
            case MOUNTAIN -> Scene.MOUNTAIN;
            case COASTAL -> Scene.COASTAL;
            case PARKLAND, WOODLAND -> Scene.PARKLAND;
        };
    }

    private static String tierLabel(TourTier tier) {
        return switch (tier) {
            case DEVELOPMENT -> "Development";
            case PRO -> "Pro";
        };
    }
}
