package com.progolf.app.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A {@link UserStore} backed by the local filesystem (spec: authentication): one {@code <id>.json} per
 * account under a configurable directory, mirroring {@code FilesystemSaveGameStore}. Writes are atomic
 * (temp file + move). Username lookups scan the directory — adequate for the V1/single-node scale; the
 * Postgres adapter replaces this behind the port. All Jackson/filesystem code lives here.
 */
@Component
public class FilesystemUserStore implements UserStore {

    private static final String EXTENSION = ".json";

    private final Path directory;
    private final ObjectMapper mapper;

    public FilesystemUserStore(@Value("${progolf.users.dir:./users}") String directory) {
        this.directory = Path.of(directory);
        this.mapper = JsonMapper.builder().findAndAddModules().build(); // jsr310 for Instant
    }

    @Override
    public void save(UserAccount account) {
        try {
            Files.createDirectories(directory);
            byte[] bytes = mapper.writeValueAsBytes(account);
            Path target = directory.resolve(account.id() + EXTENSION);
            Path temp = Files.createTempFile(directory, account.id() + "-", ".tmp");
            Files.write(temp, bytes);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store user account " + account.id(), e);
        }
    }

    @Override
    public Optional<UserAccount> findById(String id) {
        Path file = directory.resolve(id + EXTENSION);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(read(file));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(p -> p.toString().endsWith(EXTENSION))
                    .map(this::read)
                    .filter(a -> a.username().equals(username))
                    .findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan user accounts", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    private UserAccount read(Path file) {
        try {
            return mapper.readValue(Files.readAllBytes(file), UserAccount.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read user account " + file, e);
        }
    }
}
