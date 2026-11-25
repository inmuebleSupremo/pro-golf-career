package com.progolf.app.auth;

import java.util.Optional;

/**
 * The port for durable user-account storage (spec: authentication). Accounts are saved and looked up through
 * this seam; the medium (a filesystem adapter now, a database later) sits behind it, mirroring
 * {@code SaveGameStore}. Username uniqueness is enforced by {@link #create}; an availability check alone
 * cannot make concurrent registration safe.
 */
public interface UserStore {

    /** Persists {@code account}, overwriting any existing account with the same id. */
    void save(UserAccount account);

    /** Creates an account only if its username is still unused; rejects a concurrent duplicate. */
    void create(UserAccount account);

    /** The account with the given username, or empty if none. */
    Optional<UserAccount> findByUsername(String username);

    /** The account with the given id, or empty if none. */
    Optional<UserAccount> findById(String id);

    /** True if an account with the given username already exists. */
    boolean existsByUsername(String username);
}
