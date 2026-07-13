package com.progolf.app.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A refresh was attempted with a token that is missing, malformed, expired, not a refresh token, or whose
 * user no longer exists (spec: authentication); surfaces as 401.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
