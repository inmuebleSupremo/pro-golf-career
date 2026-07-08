package com.progolf.sim.population;

import com.progolf.sim.core.Rng;

/** Deterministic procedural naming for generated golfers. Cosmetic only. */
final class PlayerNames {

    private static final String[] FIRST = {
            "James", "Liam", "Noah", "Hiro", "Seve", "Ben", "Rory", "Adam", "Louis", "Min",
            "Carl", "Diego", "Anders", "Kenji", "Paul", "Ernie", "Vijay", "Tom", "Sam", "Leo"};
    private static final String[] LAST = {
            "Mackenzie", "Vasquez", "Olsen", "Tanaka", "Bergman", "Novak", "Fields", "Rossi", "Park", "Dubois",
            "Kaur", "Nilsson", "Okoro", "Reyes", "Sato", "Walsh", "Meyer", "Cole", "Bauer", "Frost"};

    private PlayerNames() {
    }

    static String first(Rng rng) {
        return FIRST[(int) Math.floorMod(rng.nextLong(), FIRST.length)];
    }

    static String last(Rng rng) {
        return LAST[(int) Math.floorMod(rng.nextLong(), LAST.length)];
    }
}
