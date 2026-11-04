package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;

/** Rating endpoints recorded when playable training actually raises an existing attribute. */
public record TrainingRatingChange(Attribute attribute, int before, int after) {
    public TrainingRatingChange {
        if (attribute == null || before < 0 || after > 100 || after <= before)
            throw new IllegalArgumentException("invalid training rating change");
    }
}
