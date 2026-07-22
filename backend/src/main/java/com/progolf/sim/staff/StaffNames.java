package com.progolf.sim.staff;

import com.progolf.sim.core.Rng;

/**
 * A small, self-contained name pool for generating staff members (spec: support-team). Kept within the staff
 * domain so it does not reach into the population domain's golfer names. Deterministic given the supplied Rng.
 */
final class StaffNames {

    private StaffNames() {
    }

    private static final String[] FIRST = {
        "Marcus", "Elena", "Diego", "Priya", "Sven", "Amara", "Hiroshi", "Nadia", "Callum", "Sofia",
        "Tobias", "Yuki", "Ravi", "Ingrid", "Mateo", "Zara", "Anders", "Leilani", "Omar", "Freya",
        "Kwame", "Bianca", "Lucas", "Mei", "Rafael"
    };

    private static final String[] LAST = {
        "Hargreaves", "Novak", "Okafor", "Reyes", "Lindqvist", "Bianchi", "Tanaka", "Petrova", "Mensah", "Aoki",
        "Fernandes", "Kaur", "Halvorsen", "Costa", "Ibrahim", "Larsson", "Rossi", "Nakamura", "Delgado", "Vasquez",
        "Schneider", "Moreau", "Adeyemi", "Kovac", "Sørensen"
    };

    // Country codes matching the golfer Nationality enum names, so the frontend labels them consistently.
    private static final String[] NATIONALITIES = {
        "USA", "GBR", "ESP", "AUS", "JPN", "KOR", "DEU", "FRA", "SWE", "ITA", "ZAF", "CAN", "IRL", "ARG", "IND", "MEX"
    };

    static String first(Rng rng) {
        return FIRST[(int) (rng.nextDouble() * FIRST.length)];
    }

    static String last(Rng rng) {
        return LAST[(int) (rng.nextDouble() * LAST.length)];
    }

    static String nationality(Rng rng) {
        return NATIONALITIES[(int) (rng.nextDouble() * NATIONALITIES.length)];
    }
}
