package com.progolf.sim.population;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Identity;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.player.Player;
import com.progolf.sim.player.ProfessionalGolfer;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds a player-created, human-controlled golfer from the player's choices (spec: golfer-creation).
 * Unlike {@link PopulationGenerator} (which seeds a random AI population), this is deterministic from its
 * inputs — the player chose this identity and build — and produces a {@link ProfessionalGolfer#human}
 * whose starting attributes derive from the chosen {@link Archetype}'s emphasis around a rookie baseline
 * (the generation lives here, per REQ-004; the emphasis is data on the archetype).
 */
public final class GolferFactory {

    private GolferFactory() {
    }

    /**
     * Creates a human-controlled golfer with the chosen identity and an archetype-shaped starting build.
     * The date of birth is derived from {@code referenceYear} so the golfer's starting age is exactly
     * {@code startAge}. Start age must be within the created-golfer bounds.
     */
    public static ProfessionalGolfer createHuman(String id, String firstName, String lastName,
                                                 Nationality nationality, int startAge, Archetype archetype,
                                                 int referenceYear) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nationality, "nationality");
        Objects.requireNonNull(archetype, "archetype");
        if (startAge < PopulationConstants.CREATION_MIN_AGE || startAge > PopulationConstants.CREATION_MAX_AGE) {
            throw new IllegalArgumentException("start age must be "
                    + PopulationConstants.CREATION_MIN_AGE + "-" + PopulationConstants.CREATION_MAX_AGE + ": " + startAge);
        }

        Attributes attributes = buildAttributes(archetype);
        LocalDate dob = LocalDate.of(referenceYear - startAge, 1, 1);
        Identity identity = new Identity(firstName, lastName, nationality, dob, archetype);

        Player player = new Player(id, identity, attributes);
        player.activate(); // CREATED -> ACTIVE

        return ProfessionalGolfer.human(id, player, "career-" + id);
    }

    /** The starting build: a rookie baseline, raised on the archetype's strengths and lowered on its weaknesses. */
    private static Attributes buildAttributes(Archetype archetype) {
        Map<Attribute, Integer> values = new EnumMap<>(Attribute.class);
        for (Attribute a : Attribute.values()) {
            int value = PopulationConstants.CREATION_BASELINE;
            if (archetype.strengths().contains(a)) {
                value += PopulationConstants.CREATION_EMPHASIS;
            } else if (archetype.weaknesses().contains(a)) {
                value -= PopulationConstants.CREATION_DEEMPHASIS;
            }
            values.put(a, Attributes.clamp(value));
        }
        return Attributes.of(values);
    }
}
