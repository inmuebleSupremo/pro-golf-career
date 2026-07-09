package com.progolf.sim.equipment;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The equipment selected for a Tournament (spec: tournament-loadout, REQ-205/211): one item per category.
 * A loadout is valid only if every selected item is owned and all categories are present, so only owned
 * equipment ever reaches play. Immutable — preparing a new loadout never alters a previous one.
 */
public record TournamentLoadout(Map<EquipmentCategory, EquipmentItem> selection) {

    public TournamentLoadout {
        Objects.requireNonNull(selection, "selection");
        selection = new EnumMap<>(selection);
    }

    /** Whether this loadout selects only owned equipment and covers every category (REQ-211). */
    public boolean isValid(EquipmentInventory inventory) {
        if (selection.size() != EquipmentCategory.values().length) {
            return false;
        }
        for (Map.Entry<EquipmentCategory, EquipmentItem> e : selection.entrySet()) {
            EquipmentItem item = e.getValue();
            if (item.category() != e.getKey() || !inventory.owns(item)) {
                return false;
            }
        }
        return true;
    }

    /** A copy of this loadout with {@code item} selected for its category. */
    public TournamentLoadout with(EquipmentItem item) {
        Map<EquipmentCategory, EquipmentItem> next = new EnumMap<>(selection);
        next.put(item.category(), item);
        return new TournamentLoadout(next);
    }

    /** The best-owned loadout: the highest-quality owned item in every category. */
    public static TournamentLoadout bestFrom(EquipmentInventory inventory) {
        Map<EquipmentCategory, EquipmentItem> selection = new EnumMap<>(EquipmentCategory.class);
        for (EquipmentCategory category : EquipmentCategory.values()) {
            inventory.bestIn(category).ifPresent(item -> selection.put(category, item));
        }
        return new TournamentLoadout(selection);
    }
}
