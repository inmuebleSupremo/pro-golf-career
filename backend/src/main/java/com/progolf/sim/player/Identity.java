package com.progolf.sim.player;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A golfer's permanent, immutable identity (REQ-002/015). Set once at creation and never changed. Names
 * support international characters and must be 1-50 characters.
 */
public record Identity(String firstName, String lastName, Nationality nationality, LocalDate dateOfBirth, Archetype archetype) {

    private static final int NAME_MIN = 1;
    private static final int NAME_MAX = 50;

    public Identity {
        firstName = requireName(firstName, "firstName");
        lastName = requireName(lastName, "lastName");
        Objects.requireNonNull(nationality, "nationality");
        Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        Objects.requireNonNull(archetype, "archetype");
    }

    private static String requireName(String value, String field) {
        Objects.requireNonNull(value, field);
        int length = value.strip().length();
        if (length < NAME_MIN || value.length() > NAME_MAX) {
            throw new IllegalArgumentException(field + " must be " + NAME_MIN + "-" + NAME_MAX + " characters");
        }
        return value;
    }

    /** Full display name. */
    public String fullName() {
        return firstName + " " + lastName;
    }
}
