package com.progolf.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** authentication: the filesystem user store round-trips accounts, looks them up, and persists across instances. */
class FilesystemUserStoreTest {

    @TempDir
    Path dir;

    private UserAccount account(String id, String username) {
        return new UserAccount(id, username, "$2a$hash", Instant.parse("2026-07-13T00:00:00Z"));
    }

    @Test
    void savesAndFindsByUsernameAndId() {
        FilesystemUserStore store = new FilesystemUserStore(dir.toString());
        UserAccount alice = account("id-alice", "alice");
        store.save(alice);

        assertThat(store.findByUsername("alice")).contains(alice);
        assertThat(store.findById("id-alice")).contains(alice);
        assertThat(store.existsByUsername("alice")).isTrue();
        assertThat(store.existsByUsername("bob")).isFalse();
        assertThat(store.findById("missing")).isEmpty();
    }

    @Test
    void accountsPersistAcrossStoreInstances() {
        new FilesystemUserStore(dir.toString()).save(account("id-carol", "carol"));

        // A fresh store over the same directory (as if after a restart) still sees the account.
        FilesystemUserStore reopened = new FilesystemUserStore(dir.toString());
        assertThat(reopened.findByUsername("carol")).isPresent();
    }
}
