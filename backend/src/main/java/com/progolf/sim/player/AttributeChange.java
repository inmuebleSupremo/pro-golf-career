package com.progolf.sim.player;

import com.progolf.sim.core.Attribute;
import java.util.Objects;

/**
 * An immutable record of a permanent attribute change to a Player (REQ-049/153): which attribute changed,
 * by how much (signed), why, and in which season. Development and aging are the only sources of permanent
 * attribute change — never tournament randomness.
 */
public record AttributeChange(Attribute attribute, int delta, Reason reason, int season) {

    public enum Reason {
        DEVELOPMENT,
        AGING
    }

    public AttributeChange {
        Objects.requireNonNull(attribute, "attribute");
        Objects.requireNonNull(reason, "reason");
    }
}
