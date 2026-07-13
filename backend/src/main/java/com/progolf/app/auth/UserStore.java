package com.progolf.app.auth;

import java.util.Optional;

/**
 * The port for durable user-account storage (spec: authentication). Accounts are saved and looked up through
 * this seam; the medium (a filesystem adapter now, a database later) sits behind it, mirroring
 * {@code SaveGameStore}. Usernames are unique — callers check {@link #existsByUsername} before creating.
 */
public interface UserStore {

    /** Persists {@code account}, overwriting any existing account with the same id. */
    void save(UserAccount account);

    /** The account with the given username, or empty if none. */
    Optional<UserAccount> findByUsername(String username);

    /** The account with the given id, or empty if none. */
    Optional<UserAccount> findById(String id);

    /** True if an account with the given username already exists. */
    boolean existsByUsername(String username);
}
