package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The deterministic Development-Point allocation policy (REQ-163): a golfer works on their whole game,
 * weakest attributes first, by the same rules for every golfer. Given the same attributes, potential and
 * points it always produces the same allocation.
 *
 * <p>This is the default every golfer gets, the player included, and it is meant to be a good default: a
 * career should progress without the player having to run a spreadsheet against a cost curve they cannot
 * see. A player-chosen focus (spec: player-control) remains available to bias development, but it is an
 * option for someone who wants it, not a tax on someone who does not.
 */
public final class AllocationPolicy {

    private AllocationPolicy() {
    }

    /**
     * Splits {@code points} evenly across every attribute that still has headroom below its potential,
     * deterministically — a golfer works on their whole game and grows into the player they could be.
     *
     * <p>Attributes already at their ceiling are skipped rather than allocated to: otherwise a golfer's best
     * attributes, which are the first to max out, would soak up every point for the rest of their career and
     * buy nothing. Points landing on a maxed attribute are the single easiest way for a season of
     * development to silently amount to nothing, which is exactly what a career must never do.
     */
    public static Map<Attribute, Integer> aiAllocate(Attributes attributes, Attributes potential, int points) {
        if (points <= 0) {
            return Map.of();
        }
        List<Attribute> focus = java.util.Arrays.stream(Attribute.values())
                .filter(a -> attributes.get(a) < potential.get(a))
                .sorted(Comparator.comparingInt(attributes::get).thenComparing(Comparator.naturalOrder()))
                .toList();
        if (focus.isEmpty()) {
            return Map.of(); // fully realised: there is nothing left to buy
        }

        Map<Attribute, Integer> allocation = new EnumMap<>(Attribute.class);
        int base = points / focus.size();
        int remainder = points % focus.size();
        for (int i = 0; i < focus.size(); i++) {
            int share = base + (i < remainder ? 1 : 0); // remainder to the strongest, deterministically
            if (share > 0) {
                allocation.put(focus.get(i), share);
            }
        }
        return allocation;
    }
}
