package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The deterministic AI Development-Point allocation policy (REQ-163). It specialises: it concentrates a
 * golfer's points on their already-strongest attributes (with a small spread), by the same rules for
 * every golfer. Given the same attributes and points it always produces the same allocation.
 */
public final class AllocationPolicy {

    private AllocationPolicy() {
    }

    /** Splits {@code points} across the golfer's top attributes (specialisation), deterministically. */
    public static Map<Attribute, Integer> aiAllocate(Attributes attributes, int points) {
        if (points <= 0) {
            return Map.of();
        }
        List<Attribute> focus = java.util.Arrays.stream(Attribute.values())
                .sorted(Comparator.comparingInt(attributes::get).reversed().thenComparing(Comparator.naturalOrder()))
                .limit(ProgressionConstants.AI_FOCUS_ATTRIBUTES)
                .toList();

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
