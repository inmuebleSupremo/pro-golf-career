package com.progolf.app.api.dto;

/** Owner-scoped view of a career's future pin-placement policy. */
public record PinPlacementStatusDto(String defaultVersion, int legacyScheduledEvents, boolean canAdoptV5) {
}
