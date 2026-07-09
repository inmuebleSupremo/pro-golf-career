package com.progolf.sim.equipment;

import java.util.Objects;

/**
 * A recorded equipment acquisition in a golfer's ownership history (spec: equipment-inventory, REQ-209):
 * the season, category, item name, and how it was acquired. Immutable; contributes to the golfer's
 * long-term identity.
 */
public record EquipmentAcquisition(int season, EquipmentCategory category, String itemName, Method method) {

    public enum Method {
        INITIAL, PURCHASE, SPONSORSHIP
    }

    public EquipmentAcquisition {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(itemName, "itemName");
        Objects.requireNonNull(method, "method");
    }
}
