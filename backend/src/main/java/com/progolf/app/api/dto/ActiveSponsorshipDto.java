package com.progolf.app.api.dto;

/**
 * The GraphQL view of a signed, currently-active sponsorship (capability graphql-api): the sponsor, its
 * industry, the per-season payment, and the seasons left to run. Projects an active
 * {@code sim.economy.SponsorshipAgreement}.
 */
public record ActiveSponsorshipDto(String sponsor, String industry, double perSeasonPayment,
                                   int seasonsRemaining) {
}
