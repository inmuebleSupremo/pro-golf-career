package com.progolf.app.api.dto;

/**
 * The GraphQL view of an equipment brand deal (capability graphql-api): the {@code brand}, the money
 * (one-off {@code signingBonus} + {@code perSeasonRetainer} for {@code durationSeasons}), the {@code gearTier}
 * of the bag it provides, {@code gearFit} in [0,1] — how well the brand's characteristic bias suits the
 * player's build — and {@code seasonsRemaining} (the full term for a pending offer; the seasons left to run
 * for the active deal). Projects {@code sim.equipment.EquipmentDeal}.
 */
public record EquipmentDealDto(String brand, double signingBonus, double perSeasonRetainer,
                               int durationSeasons, double gearTier, double gearFit, int seasonsRemaining) {
}
