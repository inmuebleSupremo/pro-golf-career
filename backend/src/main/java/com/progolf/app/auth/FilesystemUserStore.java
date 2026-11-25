package com.progolf.app.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A {@link UserStore} backed by the local filesystem (spec: authentication): one {@code <id>.json} per
 * account under a configurable directory, mirroring {@code FilesystemSaveGameStore}. Writes are atomic
 * (temp file + move). New-account creation serializes only contenders for the same username within this
 * single backend instance. Username lookups scan the directory — adequate for the current single-node scale.
 * All Jackson/filesystem code lives here.
 */
@Component
public class FilesystemUserStore implements UserStore {

    private static final String EXTENSION = ".json";

    private final Path directory;
    private final ObjectMapper mapper;
    private final ConcurrentMap<String, RegistrationLock> registrationLocks = new ConcurrentHashMap<>();

    private static final class RegistrationLock {
        int users;
    }

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
    public void create(UserAccount account) {
        RegistrationLock registration = registrationLocks.compute(account.username(), (name, existing) -> {
            RegistrationLock lock = existing == null ? new RegistrationLock() : existing;
            lock.users++;
            return lock;
        });
        try {
            synchronized (registration) {
                if (existsByUsername(account.username())) {
                    throw new UsernameTakenException(account.username());
                }
                save(account);
            }
        } finally {
            registrationLocks.compute(account.username(), (name, current) -> {
                if (current != registration) {
                    throw new IllegalStateException("Registration lock changed while in use");
                }
                return --current.users == 0 ? null : current;
            });
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
