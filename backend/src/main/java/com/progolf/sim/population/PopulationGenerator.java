package com.progolf.sim.population;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Rng;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.ControlType;
import com.progolf.sim.player.DecisionPolicy;
import com.progolf.sim.player.Identity;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.player.Player;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.shot.Strategy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministically generates a persistent, diverse population of simulation-controlled Professional
 * Golfers from the world seed hierarchy (REQ-103/120/125). Each golfer is seeded independently by index,
 * so populations are reproducible and can be replenished at fresh indices without disturbing existing
 * members. Rivalries are never authored here — they are emergent (REQ-121).
 */
public final class PopulationGenerator {

    /** A minimal default decision seam for generated golfers; richer AI decisions arrive later. */
    private static final DecisionPolicy DEFAULT_POLICY = () -> Strategy.BALANCED;

    private PopulationGenerator() {
    }

    /** Generates a population of {@link PopulationConstants#DEFAULT_SIZE} golfers. */
    public static List<ProfessionalGolfer> generate(SeedCoordinate base) {
        return generate(base, PopulationConstants.DEFAULT_SIZE);
    }

    /** Generates {@code size} golfers at indices [0, size). */
    public static List<ProfessionalGolfer> generate(SeedCoordinate base, int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0: " + size);
        }
        List<ProfessionalGolfer> golfers = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            golfers.add(generateOne(base, index));
        }
        return golfers;
    }

    /**
     * Replenishment: generates {@code count} new golfers at indices [fromIndex, fromIndex + count),
     * keeping the population sufficient as members leave. Independent of any human player (REQ-125).
     */
    public static List<ProfessionalGolfer> replenish(SeedCoordinate base, int fromIndex, int count) {
        if (fromIndex < 0 || count < 0) {
            throw new IllegalArgumentException("fromIndex and count must be >= 0");
        }
        List<ProfessionalGolfer> golfers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            golfers.add(generateOne(base, fromIndex + i));
        }
        return golfers;
    }

    /** Generates a single golfer deterministically for the given population index. */
    public static ProfessionalGolfer generateOne(SeedCoordinate base, int index) {
        Objects.requireNonNull(base, "base");
        long golferSeed = Seeds.deriveSeed(Seeds.forCoordinate(base), index);
        Rng rng = new SplitMix64Rng(golferSeed);

        Attributes attributes = generateAttributes(rng);
        Identity identity = generateIdentity(rng);

        String playerId = "golfer-" + Long.toUnsignedString(golferSeed, 16);
        Player player = new Player(playerId, identity, attributes);
        player.activate(); // CREATED -> ACTIVE

        String careerRef = "career-" + Long.toUnsignedString(golferSeed, 16);
        return ProfessionalGolfer.simulation(playerId, player, careerRef, DEFAULT_POLICY);
    }

    /**
     * Builds a diverse attribute profile: an overall skill level plus an independent per-attribute
     * deviation, so golfers differ both in overall strength and in which attributes are their best.
     */
    private static Attributes generateAttributes(Rng rng) {
        double overall = PopulationConstants.SKILL_MIN
                + rng.nextDouble() * (PopulationConstants.SKILL_MAX - PopulationConstants.SKILL_MIN);
        Map<Attribute, Integer> values = new EnumMap<>(Attribute.class);
        for (Attribute a : Attribute.values()) {
            double deviation = (rng.nextDouble() * 2.0 - 1.0) * PopulationConstants.ATTRIBUTE_SPREAD;
            int value = (int) Math.round(overall + deviation);
            values.put(a, Attributes.clamp(value));
        }
        return Attributes.of(values);
    }

    private static Identity generateIdentity(Rng rng) {
        String first = PlayerNames.first(rng);
        String last = PlayerNames.last(rng);
        Nationality nationality = Nationality.values()[(int) Math.floorMod(rng.nextLong(), Nationality.values().length)];
        Archetype archetype = Archetype.values()[(int) Math.floorMod(rng.nextLong(), Archetype.values().length)];
        int age = archetype.minStartAge()
                + (int) Math.floorMod(rng.nextLong(), archetype.maxStartAge() - archetype.minStartAge() + 1);
        int month = 1 + (int) Math.floorMod(rng.nextLong(), 12);
        int day = 1 + (int) Math.floorMod(rng.nextLong(), 28);
        LocalDate dob = LocalDate.of(PopulationConstants.REFERENCE_YEAR - age, month, day);
        return new Identity(first, last, nationality, dob, archetype);
    }
}
