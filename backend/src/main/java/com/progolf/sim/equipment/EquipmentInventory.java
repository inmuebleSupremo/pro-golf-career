package com.progolf.sim.equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A Professional Golfer's Equipment Inventory (spec: equipment-inventory, REQ-203): all owned items, with
 * an append-only ownership history. Supports future expansion — multiple items per category are allowed.
 * Ownership is recorded on every acquisition and never silently lost.
 */
public final class EquipmentInventory {

    private final List<EquipmentItem> owned = new ArrayList<>();
    private final List<EquipmentAcquisition> history = new ArrayList<>();

    /** Adds an item to the inventory and records its acquisition (REQ-209/210). */
    public void add(EquipmentItem item, int season, EquipmentAcquisition.Method method) {
        Objects.requireNonNull(item, "item");
        owned.add(item);
        history.add(new EquipmentAcquisition(season, item.category(), item.name(), method));
    }

    /** An immutable capture of the inventory (spec: world-snapshot): owned items + acquisition history. */
    public record Snapshot(List<EquipmentItem> owned, List<EquipmentAcquisition> history) {
        public Snapshot {
            owned = List.copyOf(owned);
            history = List.copyOf(history);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(new ArrayList<>(owned), new ArrayList<>(history));
    }

    public static EquipmentInventory restore(Snapshot s) {
        EquipmentInventory inv = new EquipmentInventory();
        inv.owned.addAll(s.owned());
        inv.history.addAll(s.history());
        return inv;
    }

    /** Whether the golfer owns the given item. */
    public boolean owns(EquipmentItem item) {
        return owned.contains(item);
    }

    /** All owned items in a category. */
    public List<EquipmentItem> itemsIn(EquipmentCategory category) {
        List<EquipmentItem> items = new ArrayList<>();
        for (EquipmentItem i : owned) {
            if (i.category() == category) {
                items.add(i);
            }
        }
        return items;
    }

    /** The highest-quality owned item in a category, if any. */
    public Optional<EquipmentItem> bestIn(EquipmentCategory category) {
        EquipmentItem best = null;
        for (EquipmentItem i : owned) {
            if (i.category() == category && (best == null || i.quality() > best.quality())) {
                best = i;
            }
        }
        return Optional.ofNullable(best);
    }

    /** All owned items. */
    public List<EquipmentItem> all() {
        return Collections.unmodifiableList(new ArrayList<>(owned));
    }

    /** The append-only ownership history (REQ-209). */
    public List<EquipmentAcquisition> history() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }
}
