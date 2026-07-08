package com.progolf.sim.tournament;

import java.util.Optional;

/**
 * The typed outcome of a registration attempt (REQ-089). Registration failing is an expected result,
 * not an exception — a failure carries a clear {@link Reason}.
 */
public record RegistrationResult(TournamentEntry entry, Reason reason) {

    /** The reason a registration was refused. */
    public enum Reason {
        REGISTRATION_CLOSED,
        NOT_ELIGIBLE_INACTIVE,
        DUPLICATE_ENTRY,
        FIELD_FULL
    }

    public static RegistrationResult success(TournamentEntry entry) {
        return new RegistrationResult(entry, null);
    }

    public static RegistrationResult failure(Reason reason) {
        return new RegistrationResult(null, reason);
    }

    public boolean succeeded() {
        return entry != null;
    }

    public Optional<TournamentEntry> entryIfPresent() {
        return Optional.ofNullable(entry);
    }
}
