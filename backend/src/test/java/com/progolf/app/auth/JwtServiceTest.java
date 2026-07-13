package com.progolf.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/** authentication: JwtService issues typed access/refresh tokens, decodes them, and rejects expired ones. */
class JwtServiceTest {

    private static final String SECRET = "test-only-secret-for-junit-0123456789abcdefghijklmnop";

    @Test
    void issuesAndDecodesTypedTokens() {
        JwtService jwt = new JwtService(SECRET, 900, 1209600);

        Jwt access = jwt.decode(jwt.issueAccessToken("user-1"));
        assertThat(access.getSubject()).isEqualTo("user-1");
        assertThat(jwt.typeOf(access)).isEqualTo(JwtService.ACCESS);

        Jwt refresh = jwt.decode(jwt.issueRefreshToken("user-1"));
        assertThat(jwt.typeOf(refresh)).isEqualTo(JwtService.REFRESH);
    }

    @Test
    void rejectsAnExpiredToken() {
        // Issue with a clock set an hour in the past: valid at issuance (exp after iat) but, with a 60s TTL,
        // already expired well beyond the decoder's default 60s clock skew when decoded against the real now.
        Clock anHourAgo = Clock.fixed(Clock.systemUTC().instant().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        JwtService withPastClock = new JwtService(SECRET, 60, 60, anHourAgo);
        String expired = withPastClock.issueAccessToken("user-1");

        // Decode with a real-now clock (the Spring-wired instance) — the token is expired.
        JwtService now = new JwtService(SECRET, 900, 1209600);
        assertThatThrownBy(() -> now.decode(expired)).isInstanceOf(JwtException.class);
    }
}
