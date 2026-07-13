package com.progolf.app.api.dto;

/**
 * The GraphQL view of an equipment item / upgrade offer (capability graphql-api): the name, {@code category}
 * (as its enum name), summary quality in [0,1], cost, and the four gameplay characteristics
 * (forgiveness, power, workability, feel). Projects {@code sim.equipment.EquipmentItem}.
 */
public record EquipmentItemDto(String name, String category, double quality, double cost,
                               double forgiveness, double power, double workability, double feel) {
}
