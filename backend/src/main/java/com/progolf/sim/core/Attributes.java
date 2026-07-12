package com.progolf.sim.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable holder of a golfer's nine permanent attribute values (spec: numerical-model).
 *
 * <p>Stored values are always validated to the 0-100 range. Intermediate calculation values may
 * transiently exceed these bounds, but nothing outside 0-100 is ever stored here.
 */
public final class Attributes {

    /** Inclusive lower bound for any stored attribute. */
    public static final int MIN = 0;
    /** Inclusive upper bound for any stored attribute. */
    public static final int MAX = 100;

    private final int[] values; // indexed by Attribute.ordinal()

    private Attributes(int[] values) {
        this.values = values;
    }

    /** Creates attributes from a map; every attribute must be present and within 0-100. */
    public static Attributes of(Map<Attribute, Integer> source) {
        Objects.requireNonNull(source, "source");
        int[] v = new int[Attribute.values().length];
        for (Attribute a : Attribute.values()) {
            Integer value = source.get(a);
            if (value == null) {
                throw new IllegalArgumentException("Missing attribute: " + a);
            }
            v[a.ordinal()] = requireInRange(a, value);
        }
        return new Attributes(v);
    }

    /** Creates attributes with every value set to {@code uniform} (must be within 0-100). Useful for tests. */
    public static Attributes uniform(int uniform) {
        Map<Attribute, Integer> m = new EnumMap<>(Attribute.class);
        for (Attribute a : Attribute.values()) {
            m.put(a, uniform);
        }
        return of(m);
    }

    /** Returns the stored value for {@code attribute} (0-100). */
    public int get(Attribute attribute) {
        return values[Objects.requireNonNull(attribute, "attribute").ordinal()];
    }

    /** Returns the value normalised to [0.0, 1.0]. */
    public double norm(Attribute attribute) {
        return get(attribute) / (double) MAX;
    }

    /** Returns a copy with {@code attribute} set to {@code value} (clamped to 0-100). */
    public Attributes with(Attribute attribute, int value) {
        Objects.requireNonNull(attribute, "attribute");
        int[] copy = values.clone();
        copy[attribute.ordinal()] = clamp(value);
        return new Attributes(copy);
    }

    /** Clamps an arbitrary integer to the stored range 0-100. */
    public static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }

    private static int requireInRange(Attribute a, int value) {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(
                    "Attribute " + a + " value " + value + " out of range [" + MIN + "," + MAX + "]");
        }
        return value;
    }

    /** Value equality over the stored attribute values (Attributes is an immutable value type). */
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Attributes other && java.util.Arrays.equals(values, other.values));
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(values);
    }
}
