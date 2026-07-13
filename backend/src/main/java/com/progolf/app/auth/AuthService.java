package com.progolf.app.auth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/**
 * The authentication use cases (spec: authentication): register an account (BCrypt-hashed), log in to obtain
 * a token pair, and refresh an access token. Delegates storage to {@link UserStore}, hashing to the
 * {@link PasswordEncoder}, and token handling to {@link JwtService}. Never returns or logs a plaintext
 * password.
 */
@Service
public class AuthService {

    private final UserStore userStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserStore userStore, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userStore = userStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Creates an account, storing the password as a BCrypt hash.
     *
     * @throws UsernameTakenException if the username already exists
     */
    public UserAccount register(String username, String password) {
        if (userStore.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        UserAccount account = new UserAccount(UUID.randomUUID().toString(), username,
                passwordEncoder.encode(password), Instant.now());
        userStore.save(account);
        return account;
    }

    /**
     * Verifies credentials and issues an access + refresh token pair.
     *
     * @throws InvalidCredentialsException if the username is unknown or the password is wrong
     */
    public TokenPair login(String username, String password) {
        UserAccount account = userStore.findByUsername(username)
                .filter(a -> passwordEncoder.matches(password, a.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return new TokenPair(jwtService.issueAccessToken(account.id()), jwtService.issueRefreshToken(account.id()));
    }

    /**
     * Exchanges a valid refresh token for a new access token.
     *
     * @throws InvalidRefreshTokenException if the token is invalid/expired, is not a refresh token, or its
     *     user no longer exists
     */
    public String refresh(String refreshToken) {
        Jwt jwt;
        try {
            jwt = jwtService.decode(refreshToken);
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException();
        }
        if (!JwtService.REFRESH.equals(jwtService.typeOf(jwt))) {
            throw new InvalidRefreshTokenException();
        }
        String userId = jwt.getSubject();
        if (userId == null || userStore.findById(userId).isEmpty()) {
            throw new InvalidRefreshTokenException();
        }
        return jwtService.issueAccessToken(userId);
    }
}
