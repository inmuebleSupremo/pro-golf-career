package com.progolf.sim.progression;

import com.progolf.sim.player.AttributeChange;
import java.util.List;

/** Facts produced by one authoritative weekly preparation commit. */
public record CareerPreparationOutcome(boolean committed, PreparationFocus focus, int season, int week,
                                       double baseProgress, double simulatedBonusProgress, double progressAwarded,
                                       List<AttributeChange> attributeImprovements,
                                       double fatigueBefore, double fatigueAfter, boolean readinessPending) {
    public CareerPreparationOutcome {
        attributeImprovements = List.copyOf(attributeImprovements);
    }
}
