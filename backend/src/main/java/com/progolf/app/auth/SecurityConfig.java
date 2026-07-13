package com.progolf.app.auth;

import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The security configuration (spec: authentication, design D2/D5): BCrypt password hashing, a stateless
 * JWT-gated filter chain, and the resource-server decoder for the API. The decoder here is strict — it
 * accepts only access tokens ({@code type=access}) — so a refresh token cannot reach {@code /graphql};
 * {@link JwtService} keeps its own lenient decoder for decoding refresh tokens during refresh.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String jwtSecret;

    public SecurityConfig(@Value("${progolf.auth.jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The resource-server decoder: validates signature and expiry (the default validators) and additionally
     * requires the {@code type=access} claim, so a refresh token presented at the API is rejected.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> accessOnly =
                new JwtClaimValidator<String>(JwtService.TYPE_CLAIM, JwtService.ACCESS::equals);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), accessOnly));
        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // token-based, no cookies/session
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Permit internal ERROR/FORWARD dispatches so an exception from a permitted endpoint
                        // renders its status (e.g. 409) instead of the /error forward being gated to 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/auth/**", "/actuator/health", "/graphiql", "/graphiql/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
