package com.progolf.sim.world;

import com.progolf.sim.course.Course;
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

    /** The Pro tour's season-ending Tour Championship location — fixed (a nod to East Lake, Atlanta). */
    static final String PRO_CHAMPIONSHIP_LOCATION = "Georgia";

    /** One fixed Pro-tour event: a stable name + location so the top tour reads the same every season. */
    record ProEvent(String name, String location) {
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
            new ProEvent("Lone Star Invitational", "Texas"),               // ~ Valero Texas Open
            new ProEvent("Coastal Heritage Classic", "South Carolina"),    // ~ RBC Heritage
            new ProEvent("Sunshine State Showdown", "Florida"),            // ~ Florida spring swing
            new ProEvent("Queen City Championship", "North Carolina"),     // ~ Truist Championship (Quail Hollow)
            new ProEvent("Irish Links Open", "Ireland"),                   // ~ Irish Open
            new ProEvent("Great Lakes Classic", "Michigan"),               // ~ Detroit-area events
            new ProEvent("Canadian National Open", "Ontario"),             // ~ RBC Canadian Open
            new ProEvent("New England Invitational", "Connecticut"),       // ~ Travelers Championship
            new ProEvent("Prairie State Open", "Illinois"),                // ~ John Deere Classic
            new ProEvent("Links of Britain Championship", "Scotland"),     // ~ Scottish Open / Open window
            new ProEvent("Twin Cities Classic", "Minnesota"),              // ~ 3M Open
            new ProEvent("Old North State Championship", "North Carolina"), // ~ Wyndham Championship
            new ProEvent("Mississippi Valley Invitational", "Tennessee"),  // ~ FedEx St. Jude (Memphis)
            new ProEvent("Blue Ridge Mountain Open", "North Carolina"),    // ~ Asheville-area event
            new ProEvent("Desert Mountain Challenge", "Utah"),             // ~ Bank of Utah Championship
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

    private static String tierLabel(TourTier tier) {
        return switch (tier) {
            case DEVELOPMENT -> "Development";
            case PRO -> "Pro";
        };
    }
}
