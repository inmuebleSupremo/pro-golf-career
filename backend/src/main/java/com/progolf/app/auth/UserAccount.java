package com.progolf.app.auth;

import java.time.Instant;
import java.util.Objects;

/**
 * A registered user account (spec: authentication). The password is held only as a BCrypt hash — the
 * plaintext is never stored. {@code id} is a stable, opaque identifier (the JWT subject and, later, the
 * owner of saves and sessions); {@code username} is the unique login handle.
 */
public record UserAccount(String id, String username, String passwordHash, Instant createdAt) {

    public UserAccount {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
