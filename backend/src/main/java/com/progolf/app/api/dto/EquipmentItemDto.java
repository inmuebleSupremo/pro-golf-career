package com.progolf.app.api.dto;

/**
 * The GraphQL view of an equipment item / upgrade offer (capability graphql-api): the name, {@code category}
 * (as its enum name), the {@code brand} display name, summary {@code quality} in [0,1] (the overall tier),
 * cost, the four gameplay characteristics (forgiveness, power, workability, feel, each in [0,1]), and
 * {@code fit} in [0,1] — how well the item's shape suits the player's build (0.5 neutral). Projects
 * {@code sim.equipment.EquipmentItem} scored against the player's attributes.
 */
public record EquipmentItemDto(String name, String category, String brand, double quality, double cost,
                               double forgiveness, double power, double workability, double feel, double fit) {
}
