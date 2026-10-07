package com.progolf.app.api.dto;
public record ClubReachDto(String club, String label, double nominalCarry, double normalReach,
                           java.util.List<ShotFamilyAvailabilityDto> families) {
    public ClubReachDto(String club, String label, double nominalCarry, double normalReach) {
        this(club, label, nominalCarry, normalReach, java.util.List.of());
    }
}
