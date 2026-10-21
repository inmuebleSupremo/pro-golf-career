package com.progolf.app.api.dto;

/** Player-selected physical putting line and level-ground pace. */
public record PhysicalPuttIntentInput(AimPointInput direction, double intendedRollFeet,
                                      String expectedShotRevision) {
}
