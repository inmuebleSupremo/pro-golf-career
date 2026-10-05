package com.progolf.app.api.dto;

import java.util.List;

/** A read-only, current-state view of the human player's actionable career attention. */
public record CareerInboxDto(List<CareerInboxItemDto> items) {
    public CareerInboxDto {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
