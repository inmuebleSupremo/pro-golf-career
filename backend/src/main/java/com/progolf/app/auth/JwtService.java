package com.progolf.app.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Issues and validates JWTs (spec: authentication, design D3): access and refresh tokens signed with a
 * single symmetric secret via Spring Security's Nimbus encoder/decoder — no third-party JWT library. Each
 * token carries the user id as its subject and a {@code type} claim ({@link #ACCESS}/{@link #REFRESH}) so an
 * access token cannot be used to refresh and a refresh token cannot reach the API.
 */
@Service
public class JwtService {

    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";
    /** The claim distinguishing an access token from a refresh token. */
    public static final String TYPE_CLAIM = "type";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${progolf.auth.jwt.secret}") String secret,
                      @Value("${progolf.auth.jwt.access-ttl-seconds:900}") long accessTtlSeconds,
                      @Value("${progolf.auth.jwt.refresh-ttl-seconds:1209600}") long refreshTtlSeconds) {
        this(secret, accessTtlSeconds, refreshTtlSeconds, Clock.systemUTC());
    }

    /** Test seam: a fixed clock lets a test issue a token dated in the past to exercise expiry. */
    JwtService(String secret, long accessTtlSeconds, long refreshTtlSeconds, Clock clock) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.clock = clock;
    }

    /** A short-lived access token for the given user id. */
    public String issueAccessToken(String userId) {
        return issue(userId, ACCESS, accessTtlSeconds);
    }

    /** A longer-lived refresh token for the given user id. */
    public String issueRefreshToken(String userId) {
        return issue(userId, REFRESH, refreshTtlSeconds);
    }

    /**
     * Decodes and verifies a token (signature + expiry), returning its claims.
     *
     * @throws org.springframework.security.oauth2.jwt.JwtException if the token is malformed, unsigned by our
     *     key, or expired
     */
    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    /** The {@code type} claim of a decoded token ({@link #ACCESS} or {@link #REFRESH}). */
    public String typeOf(Jwt jwt) {
        return jwt.getClaimAsString(TYPE_CLAIM);
    }

    private String issue(String userId, String type, long ttlSeconds) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plus(ttlSeconds, ChronoUnit.SECONDS))
                .claim(TYPE_CLAIM, type)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
