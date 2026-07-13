package com.progolf.app.api.dto;

/**
 * The GraphQL view of a pending sponsorship offer (capability graphql-api): the sponsor, the per-season
 * payment, the signing bonus, the duration in seasons, and the offer's gross value (the comparison figure the
 * acceptance policy uses). Projects {@code sim.economy.SponsorshipOffer} / its agreement.
 */
public record SponsorshipOfferDto(String sponsor, double perSeasonPayment, double signingBonus,
                                  int durationSeasons, double grossValue) {
}
