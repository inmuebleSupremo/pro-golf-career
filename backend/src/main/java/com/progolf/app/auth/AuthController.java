package com.progolf.app.auth;

import com.progolf.app.auth.dto.AccessTokenResponse;
import com.progolf.app.auth.dto.LoginRequest;
import com.progolf.app.auth.dto.RefreshRequest;
import com.progolf.app.auth.dto.RegisterRequest;
import com.progolf.app.auth.dto.RegisterResponse;
import com.progolf.app.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public authentication endpoints (spec: authentication): register, login, and refresh. The tech stack
 * keeps REST for auth; these are the only write endpoints outside GraphQL. This path is permitted by the
 * security filter chain (no token required); everything else requires a valid access token.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        UserAccount account = authService.register(request.username(), request.password());
        return new RegisterResponse(account.id(), account.username());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokens = authService.login(request.username(), request.password());
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken());
    }

    @PostMapping("/refresh")
    public AccessTokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return new AccessTokenResponse(authService.refresh(request.refreshToken()));
    }
}
