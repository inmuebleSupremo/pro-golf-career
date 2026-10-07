package com.progolf.sim.shot;

import com.progolf.sim.core.Attribute;
import java.util.EnumMap;
import java.util.Map;

/** Static individual-club catalogue translated onto the existing equipment and resolver families. */
public record ClubSpec(ClubId id, String label, Club family, double baseCarry,
                       Attribute lateralAttribute, Attribute distanceAttribute,
                       double lateralDispersion, double distanceDispersion) {
    private static final Map<ClubId, ClubSpec> CATALOGUE = catalogue();

    public ClubSpec {
        if (id == null || label == null || family == null || lateralAttribute == null || distanceAttribute == null) {
            throw new NullPointerException("club specification fields are required");
        }
        if (!Double.isFinite(baseCarry) || baseCarry <= 0 || !Double.isFinite(lateralDispersion)
                || !Double.isFinite(distanceDispersion)) {
            throw new IllegalArgumentException("club calibration must be finite and positive");
        }
    }

    public static ClubSpec of(ClubId id) {
        ClubSpec spec = CATALOGUE.get(id);
        if (spec == null) throw new IllegalArgumentException("unknown club id: " + id);
        return spec;
    }

    public static ClubSpec forLegacy(Club club) {
        return switch (club) {
            case DRIVER -> of(ClubId.DRIVER);
            case FAIRWAY_WOOD -> of(ClubId.THREE_WOOD);
            case HYBRID -> of(ClubId.THREE_HYBRID);
            case IRON -> of(ClubId.SIX_IRON);
            case WEDGE -> of(ClubId.GAP_WEDGE);
            case PUTTER -> of(ClubId.PUTTER);
        };
    }

    public static java.util.List<ClubSpec> all() { return java.util.List.copyOf(CATALOGUE.values()); }

    private static Map<ClubId, ClubSpec> catalogue() {
        Map<ClubId, ClubSpec> c = new EnumMap<>(ClubId.class);
        add(c, ClubId.DRIVER, "Driver", Club.DRIVER, 290);
        add(c, ClubId.THREE_WOOD, "3 Wood", Club.FAIRWAY_WOOD, 245);
        add(c, ClubId.FIVE_WOOD, "5 Wood", Club.FAIRWAY_WOOD, 230);
        add(c, ClubId.THREE_HYBRID, "3 Hybrid", Club.HYBRID, 220);
        add(c, ClubId.FOUR_HYBRID, "4 Hybrid", Club.HYBRID, 210);
        add(c, ClubId.FOUR_IRON, "4 Iron", Club.IRON, 200);
        add(c, ClubId.FIVE_IRON, "5 Iron", Club.IRON, 190);
        add(c, ClubId.SIX_IRON, "6 Iron", Club.IRON, 180);
        add(c, ClubId.SEVEN_IRON, "7 Iron", Club.IRON, 170);
        add(c, ClubId.EIGHT_IRON, "8 Iron", Club.IRON, 160);
        add(c, ClubId.NINE_IRON, "9 Iron", Club.IRON, 150);
        add(c, ClubId.PITCHING_WEDGE, "Pitching Wedge", Club.WEDGE, 135);
        add(c, ClubId.GAP_WEDGE, "Gap Wedge", Club.WEDGE, 110);
        add(c, ClubId.SAND_WEDGE, "Sand Wedge", Club.WEDGE, 90);
        add(c, ClubId.PUTTER, "Putter", Club.PUTTER, 20);
        return Map.copyOf(c);
    }

    private static void add(Map<ClubId, ClubSpec> catalogue, ClubId id, String label, Club family, double carry) {
        catalogue.put(id, new ClubSpec(id, label, family, carry, family.lateralAttribute(), family.distanceAttribute(),
                family.lateralDispersion(), family.distanceDispersion()));
    }
}
