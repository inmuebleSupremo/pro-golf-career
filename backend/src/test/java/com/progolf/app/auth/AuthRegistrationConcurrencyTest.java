package com.progolf.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthRegistrationConcurrencyTest {

    @TempDir
    Path directory;

    @Test
    void simultaneousRegistrationsOfOneUsernameCreateOneAccount() throws Exception {
        FilesystemUserStore store = new FilesystemUserStore(directory.toString());
        AuthService service = new AuthService(store, synchronizedEncoding(), null);

        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Optional<UserAccount>> attempt = () -> {
                try {
                    return Optional.of(service.register("same-user", "password-123"));
                } catch (UsernameTakenException rejected) {
                    return Optional.empty();
                }
            };
            var firstAttempt = pool.submit(attempt);
            var secondAttempt = pool.submit(attempt);
            Optional<UserAccount> first = firstAttempt.get(15, TimeUnit.SECONDS);
            Optional<UserAccount> second = secondAttempt.get(15, TimeUnit.SECONDS);

            assertThat(first.stream().count() + second.stream().count()).isEqualTo(1);
            assertThat(store.findByUsername("same-user")).contains(first.orElseGet(second::orElseThrow));
            try (var files = Files.list(directory)) {
                assertThat(files.filter(path -> path.toString().endsWith(".json")).count()).isEqualTo(1);
            }
        }
    }

    @Test
    void differentUsernamesCanRegisterConcurrently() throws Exception {
        FilesystemUserStore store = new FilesystemUserStore(directory.toString());
        AuthService service = new AuthService(store, synchronizedEncoding(), null);

        try (var pool = Executors.newFixedThreadPool(2)) {
            var firstAttempt = pool.submit(() -> service.register("first-user", "password-123"));
            var secondAttempt = pool.submit(() -> service.register("second-user", "password-456"));
            UserAccount first = firstAttempt.get(15, TimeUnit.SECONDS);
            UserAccount second = secondAttempt.get(15, TimeUnit.SECONDS);

            assertThat(first.id()).isNotEqualTo(second.id());
            assertThat(store.findByUsername("first-user")).contains(first);
            assertThat(store.findByUsername("second-user")).contains(second);
            try (var files = Files.list(directory)) {
                assertThat(files.filter(path -> path.toString().endsWith(".json")).count()).isEqualTo(2);
            }
        }
    }

    private PasswordEncoder synchronizedEncoding() {
        CyclicBarrier bothCheckedAvailability = new CyclicBarrier(2);
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                String hash = bcrypt.encode(rawPassword);
                try {
                    bothCheckedAvailability.await(10, TimeUnit.SECONDS);
                } catch (Exception failure) {
                    throw new IllegalStateException("Registrations did not reach encoding together", failure);
                }
                return hash;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return bcrypt.matches(rawPassword, encodedPassword);
            }
        };
    }
}
