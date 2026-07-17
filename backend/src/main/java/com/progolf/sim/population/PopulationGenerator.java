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
import com.progolf.sim.progression.Maturity;
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

    /**
     * The background archetypes the AI population is drawn from (for its starting-age routing). Pinned here
     * so the create-your-golfer playing-style archetypes can be added to {@link Archetype} without changing
     * the generated world (reproducibility).
     */
    private static final Archetype[] POPULATION_ARCHETYPES = {
            Archetype.GRASS_ROOTS_TALENT, Archetype.TOP_COLLEGE_GRADUATE, Archetype.FUTURE_PRODIGY};

    private PopulationGenerator() {
    }

    /** Generates a population of {@link PopulationConstants#DEFAULT_SIZE} golfers. */
    public static List<ProfessionalGolfer> generate(SeedCoordinate base) {
        return generate(base, PopulationConstants.DEFAULT_SIZE);
    }

    /**
     * Generates {@code size} golfers at indices [0, size) to seed a world: drawn across the full spread of
     * professional ages, so the tour opens with rookies, players in their prime and veterans rather than a
     * single cohort of teenagers that would then age in lockstep with nobody coming through behind them.
     */
    public static List<ProfessionalGolfer> generate(SeedCoordinate base, int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0: " + size);
        }
        List<ProfessionalGolfer> golfers = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            golfers.add(generateOne(base, index, true));
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

    /**
     * Generates a single golfer deterministically for the given population index, at the entry age — a
     * rookie turning professional. This is the replenishment path: golfers arriving in an existing world
     * come through the bottom, they do not appear mid-career.
     */
    public static ProfessionalGolfer generateOne(SeedCoordinate base, int index) {
        return generateOne(base, index, false);
    }

    /**
     * @param acrossTourAges when true the golfer is drawn from the full spread of professional ages rather
     *     than the entry age, so a world can be seeded with an age structure — rookies, golfers in their
     *     prime, and veterans on the way down, all at once.
     */
    private static ProfessionalGolfer generateOne(SeedCoordinate base, int index, boolean acrossTourAges) {
        Objects.requireNonNull(base, "base");
        long golferSeed = Seeds.deriveSeed(Seeds.forCoordinate(base), index);
        Rng rng = new SplitMix64Rng(golferSeed);

        // Talent first: a golfer is generated as the player they could become, and their attributes today are
        // that ceiling discounted back to their age (spec: player-development).
        Attributes potential = generatePotential(rng);
        Identity identity = generateIdentity(rng, acrossTourAges);
        int age = PopulationConstants.REFERENCE_YEAR - identity.dateOfBirth().getYear();
        Attributes attributes = Maturity.abilityAt(potential, age);

        String playerId = "golfer-" + Long.toUnsignedString(golferSeed, 16);
        Player player = new Player(playerId, identity, attributes, potential);
        player.activate(); // CREATED -> ACTIVE

        String careerRef = "career-" + Long.toUnsignedString(golferSeed, 16);
        // Innate strategic disposition, fixed at generation, so the field plays a spread of styles instead of
        // a uniform Balanced (spec: golfer-population). Taken from potential — a golfer's style follows the
        // shape of their talent, so it does not drift as they grow into it.
        Strategy disposition = StrategyDisposition.fromAttributes(potential);
        DecisionPolicy policy = () -> disposition;
        return ProfessionalGolfer.simulation(playerId, player, careerRef, policy);
    }

    /**
     * Builds a diverse potential profile: an overall ceiling plus an independent per-attribute deviation, so
     * golfers differ both in how good they can become and in which attributes will be their best.
     */
    private static Attributes generatePotential(Rng rng) {
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

    private static Identity generateIdentity(Rng rng, boolean acrossTourAges) {
        String first = PlayerNames.first(rng);
        String last = PlayerNames.last(rng);
        Nationality nationality = Nationality.values()[(int) Math.floorMod(rng.nextLong(), Nationality.values().length)];
        Archetype archetype = POPULATION_ARCHETYPES[(int) Math.floorMod(rng.nextLong(), POPULATION_ARCHETYPES.length)];
        int minAge = acrossTourAges ? PopulationConstants.SEED_AGE_MIN : archetype.minStartAge();
        int maxAge = acrossTourAges ? PopulationConstants.SEED_AGE_MAX : archetype.maxStartAge();
        int age = minAge + (int) Math.floorMod(rng.nextLong(), maxAge - minAge + 1);
        int month = 1 + (int) Math.floorMod(rng.nextLong(), 12);
        int day = 1 + (int) Math.floorMod(rng.nextLong(), 28);
        LocalDate dob = LocalDate.of(PopulationConstants.REFERENCE_YEAR - age, month, day);
        return new Identity(first, last, nationality, dob, archetype);
    }
}
