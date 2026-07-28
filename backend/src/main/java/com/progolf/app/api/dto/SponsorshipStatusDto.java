package com.progolf.app.api.dto;

import java.util.List;

/**
 * The GraphQL view of the player's sponsorship book (capability graphql-api): the {@code maxConcurrent} cap
 * on how many sponsorships may be held at once, and the currently-active agreements. When {@code active}
 * fills the cap, no further offer can be signed until one expires — this lets the UI explain that rather than
 * leave an offer un-signable for no visible reason.
 */
public record SponsorshipStatusDto(int maxConcurrent, List<ActiveSponsorshipDto> active) {

    public SponsorshipStatusDto {
        active = active == null ? List.of() : List.copyOf(active);
    }
}
