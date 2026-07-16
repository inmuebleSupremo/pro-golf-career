package com.progolf.sim.world;

import com.progolf.sim.course.Course;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;

/**
 * Deterministic, realistic-flavoured display names for scheduled events (spec: world-schedule). The four
 * majors carry fixed, evocative fictional names (a nod to the real majors, no licensing exposure), ordered
 * by their place in the season; tour championships are named for their tour; signature and regular events
 * take their venue's name. Pure and stable — the same event names the same way wherever it appears
 * (upcoming schedule or a completed result).
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

    /** The display name for an event; {@code majorOrdinal} is its 0-based place among the season's majors. */
    static String name(EventPrestige prestige, TourTier tier, Course course, int majorOrdinal) {
        return switch (prestige) {
            case MAJOR -> MAJOR_NAMES[Math.floorMod(majorOrdinal, MAJOR_NAMES.length)];
            case TOUR_CHAMPIONSHIP -> tierLabel(tier) + " Tour Championship";
            case SIGNATURE -> "The " + course.identity().name() + " Invitational";
            case REGULAR -> "The " + course.identity().name() + " Open";
        };
    }

    private static String tierLabel(TourTier tier) {
        return switch (tier) {
            case DEVELOPMENT -> "Development";
            case SECONDARY -> "Secondary";
            case PRIMARY -> "Primary";
            case ELITE -> "Elite";
        };
    }
}
