package com.progolf.app.api.dto;

/** One aggregated, current-state Inbox item. The client owns source-specific copy and routing. */
public record CareerInboxItemDto(CareerInboxKind kind, int count) {
}
