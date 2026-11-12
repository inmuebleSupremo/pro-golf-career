package com.progolf.sim.world;

import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.tournament.EventPrestige;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Version 1 of the permanent Development Tour calendar and its lightweight venue recipes. */
public final class DevelopmentTourCatalogue {
    public static final int VERSION = 1;

    public enum HostPolicy { FIXED, ROTATING }

    public record Event(String id, String name, String location, String country, List<String> eligibleRegions,
                        int week, EventPrestige prestige, EnvironmentClassification classification,
                        HostPolicy hostPolicy) {
        public Event {
            eligibleRegions = List.copyOf(eligibleRegions);
            if (eligibleRegions.isEmpty()) throw new IllegalArgumentException("An event needs an eligible region");
        }
    }

    public record Venue(String id, String eventId, int ordinal, String region, String country,
                        EnvironmentClassification classification) { }

    private static final List<Event> EVENTS = List.of(
            event("carolina-gateway", "Carolina Gateway Open", "North Carolina, USA", "USA", 1, EventPrestige.REGULAR, EnvironmentClassification.PARKLAND, HostPolicy.ROTATING, "North Carolina"),
            event("sandhills-classic", "Sandhills Classic", "Pinehurst region, North Carolina, USA", "USA", 2, EventPrestige.REGULAR, EnvironmentClassification.HEATHLAND, HostPolicy.FIXED, "Pinehurst region, North Carolina"),
            event("lowcountry-coastal", "Lowcountry Coastal Open", "Kiawah Island, South Carolina, USA", "USA", 3, EventPrestige.REGULAR, EnvironmentClassification.LINKS, HostPolicy.ROTATING, "Kiawah Island, South Carolina"),
            event("piedmont-challenge", "Piedmont Challenge", "North Carolina / Georgia, USA", "USA", 4, EventPrestige.REGULAR, EnvironmentClassification.WOODLAND, HostPolicy.ROTATING, "North Carolina", "Georgia"),
            event("southeastern-invitational", "Southeastern Invitational", "Georgia, USA", "USA", 5, EventPrestige.SIGNATURE, EnvironmentClassification.PARKLAND, HostPolicy.FIXED, "Georgia"),
            event("ohio-valley", "Ohio Valley Open", "Ohio, USA", "USA", 8, EventPrestige.REGULAR, EnvironmentClassification.PARKLAND, HostPolicy.ROTATING, "Ohio"),
            event("lake-michigan-dunes", "Lake Michigan Dunes Classic", "Michigan, USA", "USA", 9, EventPrestige.REGULAR, EnvironmentClassification.LINKS, HostPolicy.ROTATING, "Michigan"),
            event("pine-barrens", "Pine Barrens Open", "New Jersey, USA", "USA", 10, EventPrestige.REGULAR, EnvironmentClassification.HEATHLAND, HostPolicy.ROTATING, "New Jersey"),
            event("hudson-valley", "Hudson Valley Open", "New York, USA", "USA", 12, EventPrestige.REGULAR, EnvironmentClassification.WOODLAND, HostPolicy.ROTATING, "New York"),
            event("westchester-championship", "Westchester Championship", "New York, USA", "USA", 13, EventPrestige.REGULAR, EnvironmentClassification.PARKLAND, HostPolicy.ROTATING, "New York"),
            event("ardennes-woodland", "Ardennes Woodland Open", "Belgium", "Belgium", 15, EventPrestige.REGULAR, EnvironmentClassification.WOODLAND, HostPolicy.ROTATING, "Ardennes"),
            event("surrey-heath", "Surrey Heath Invitational", "Surrey, England", "England", 17, EventPrestige.SIGNATURE, EnvironmentClassification.HEATHLAND, HostPolicy.ROTATING, "Surrey"),
            event("northumbria-links", "Northumbria Links Classic", "Northumberland, England", "England", 18, EventPrestige.REGULAR, EnvironmentClassification.LINKS, HostPolicy.ROTATING, "Northumberland"),
            event("irish-midlands", "Irish Midlands Open", "County Kildare, Ireland", "Ireland", 19, EventPrestige.REGULAR, EnvironmentClassification.HEATHLAND, HostPolicy.ROTATING, "County Kildare"),
            event("fife-coast", "Fife Coast Championship", "Fife, Scotland", "Scotland", 20, EventPrestige.REGULAR, EnvironmentClassification.LINKS, HostPolicy.ROTATING, "Fife"),
            event("pacific-coast", "Pacific Coast Classic", "Monterey, California, USA", "USA", 23, EventPrestige.REGULAR, EnvironmentClassification.LINKS, HostPolicy.ROTATING, "Monterey, California"),
            event("coachella-valley", "Coachella Valley Classic", "California, USA", "USA", 24, EventPrestige.REGULAR, EnvironmentClassification.DESERT, HostPolicy.ROTATING, "California"),
            event("sonoran-invitational", "Sonoran Invitational", "Arizona, USA", "USA", 25, EventPrestige.SIGNATURE, EnvironmentClassification.DESERT, HostPolicy.ROTATING, "Arizona"),
            event("rio-grande-valley", "Rio Grande Valley Open", "Albuquerque, New Mexico, USA", "USA", 26, EventPrestige.REGULAR, EnvironmentClassification.PARKLAND, HostPolicy.ROTATING, "Albuquerque, New Mexico"),
            event("development-championship", "Development Tour Championship", "Carolinas, USA", "USA", 29, EventPrestige.TOUR_CHAMPIONSHIP, EnvironmentClassification.PARKLAND, HostPolicy.FIXED, "Carolinas")
    );
    private static final Map<String, Event> BY_ID;
    private static final Map<Integer, Event> BY_WEEK;
    private static final List<Venue> VENUES;
    private static final Map<String, List<Integer>> ELIGIBLE_INDICES;

    static {
        Map<String, Event> byId = new LinkedHashMap<>();
        Map<Integer, Event> byWeek = new LinkedHashMap<>();
        Map<String, List<Integer>> eligible = new LinkedHashMap<>();
        List<Venue> venues = new ArrayList<>();
        for (Event event : EVENTS) {
            if (byId.put(event.id(), event) != null || byWeek.put(event.week(), event) != null) {
                throw new IllegalStateException("Duplicate Development Tour catalogue identity");
            }
            List<Integer> indices = new ArrayList<>();
            int capacity = event.hostPolicy() == HostPolicy.FIXED ? 1 : 3;
            for (int ordinal = 0; ordinal < capacity; ordinal++) {
                indices.add(venues.size());
                venues.add(new Venue("dev-" + event.id() + "-" + ordinal, event.id(), ordinal,
                        event.eligibleRegions().get(ordinal % event.eligibleRegions().size()),
                        event.country(), event.classification()));
            }
            eligible.put(event.id(), List.copyOf(indices));
        }
        BY_ID = Map.copyOf(byId);
        BY_WEEK = Map.copyOf(byWeek);
        VENUES = List.copyOf(venues);
        ELIGIBLE_INDICES = Map.copyOf(eligible);
    }

    private DevelopmentTourCatalogue() { }

    private static Event event(String id, String name, String location, String country, int week,
                               EventPrestige prestige, EnvironmentClassification classification,
                               HostPolicy hostPolicy, String... regions) {
        return new Event(id, name, location, country, List.of(regions), week, prestige, classification, hostPolicy);
    }

    public static List<Event> events() { return EVENTS; }
    public static List<Venue> venues() { return VENUES; }
    public static Event forWeek(int week) { return Objects.requireNonNull(BY_WEEK.get(week), "No catalogue event for week " + week); }
    public static Event forId(String id) { return Objects.requireNonNull(BY_ID.get(id), "Unknown catalogue event " + id); }
    public static List<Integer> eligibleIndices(String id) { return Objects.requireNonNull(ELIGIBLE_INDICES.get(id), "Unknown catalogue event " + id); }
    public static Venue venue(int index) { return VENUES.get(index); }

    /** Seeded starting position, then a three-season cycle; a venue returns after the others have hosted. */
    public static int selectedVenueIndex(Event event, long worldSeed, int season) {
        if (season < 1) throw new IllegalArgumentException("season must be positive");
        List<Integer> eligible = eligibleIndices(event.id());
        int offset = Math.floorMod(Seeds.deriveSeed(worldSeed ^ 0x444556484f5354L,
                event.id().hashCode()), eligible.size());
        return eligible.get((offset + season - 1) % eligible.size());
    }
}
