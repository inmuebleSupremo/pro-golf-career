package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;
import java.util.List;

/** One exclusive use of a career week's preparation opportunity. */
public enum PreparationFocus {
    DRIVING(List.of(Attribute.DRIVING_ACCURACY, Attribute.DRIVING_DISTANCE)),
    IRONS(List.of(Attribute.IRONS_ACCURACY, Attribute.IRONS_CONTROL)),
    WEDGES(List.of(Attribute.WEDGES)),
    PUTTING(List.of(Attribute.PUTTING_ACCURACY, Attribute.PUTTING_PROXIMITY)),
    TOURNAMENT_PREPARATION(List.of()),
    RECOVERY(List.of());

    private final List<Attribute> attributes;

    public enum Category { TECHNICAL_TRAINING, TOURNAMENT_PREPARATION, RECOVERY }

    PreparationFocus(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public List<Attribute> attributes() {
        return attributes;
    }

    public boolean technical() {
        return !attributes.isEmpty();
    }

    public Category category() {
        return technical() ? Category.TECHNICAL_TRAINING
                : this == TOURNAMENT_PREPARATION ? Category.TOURNAMENT_PREPARATION : Category.RECOVERY;
    }
}
