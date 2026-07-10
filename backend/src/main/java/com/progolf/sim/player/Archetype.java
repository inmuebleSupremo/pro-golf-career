package com.progolf.sim.player;

import static com.progolf.sim.core.Attribute.COMPOSURE;
import static com.progolf.sim.core.Attribute.COURSE_MANAGEMENT;
import static com.progolf.sim.core.Attribute.DRIVING_ACCURACY;
import static com.progolf.sim.core.Attribute.DRIVING_DISTANCE;
import static com.progolf.sim.core.Attribute.IRONS_ACCURACY;
import static com.progolf.sim.core.Attribute.IRONS_CONTROL;
import static com.progolf.sim.core.Attribute.PUTTING_ACCURACY;
import static com.progolf.sim.core.Attribute.PUTTING_PROXIMITY;
import static com.progolf.sim.core.Attribute.WEDGES;

import com.progolf.sim.core.Attribute;
import java.util.Set;

/**
 * The starting archetype chosen at career creation (REQ-004). Identity records which archetype a golfer
 * began from. An archetype declares a starting-age range and, for player-created golfers, an attribute
 * emphasis — the attributes it is strong and weak in — from which the population domain derives the
 * starting build (the generation itself lives there, per REQ-004). Archetype is immutable after creation.
 *
 * <p>The three background archetypes (used to route the AI population's starting age) carry a neutral
 * build; the playing-style archetypes are the create-your-golfer choices.
 */
public enum Archetype {
    GRASS_ROOTS_TALENT(16, 20, Set.of(), Set.of()),
    TOP_COLLEGE_GRADUATE(21, 22, Set.of(), Set.of()),
    FUTURE_PRODIGY(16, 18, Set.of(), Set.of()),
    POWER_HITTER(18, 24, Set.of(DRIVING_DISTANCE, IRONS_CONTROL), Set.of(PUTTING_ACCURACY, WEDGES)),
    PRECISION_PLAYER(18, 26, Set.of(DRIVING_ACCURACY, IRONS_ACCURACY), Set.of(DRIVING_DISTANCE)),
    SHORT_GAME_ARTIST(18, 28, Set.of(WEDGES, PUTTING_ACCURACY, PUTTING_PROXIMITY), Set.of(DRIVING_DISTANCE)),
    ALL_ROUNDER(18, 26, Set.of(), Set.of()),
    MENTAL_FORTRESS(20, 30, Set.of(COMPOSURE, COURSE_MANAGEMENT), Set.of(DRIVING_DISTANCE));

    private final int minStartAge;
    private final int maxStartAge;
    private final Set<Attribute> strengths;
    private final Set<Attribute> weaknesses;

    Archetype(int minStartAge, int maxStartAge, Set<Attribute> strengths, Set<Attribute> weaknesses) {
        this.minStartAge = minStartAge;
        this.maxStartAge = maxStartAge;
        this.strengths = Set.copyOf(strengths);
        this.weaknesses = Set.copyOf(weaknesses);
    }

    /** Inclusive minimum starting age for this archetype. */
    public int minStartAge() {
        return minStartAge;
    }

    /** Inclusive maximum starting age for this archetype. */
    public int maxStartAge() {
        return maxStartAge;
    }

    /** Attributes this archetype starts stronger in (raised above the rookie baseline). */
    public Set<Attribute> strengths() {
        return strengths;
    }

    /** Attributes this archetype starts weaker in (lowered below the rookie baseline). */
    public Set<Attribute> weaknesses() {
        return weaknesses;
    }
}
