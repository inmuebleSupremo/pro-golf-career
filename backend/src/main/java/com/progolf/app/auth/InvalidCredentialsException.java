package com.progolf.app.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Login failed because the username is unknown or the password is wrong (spec: authentication). The message
 * is deliberately generic so it does not reveal which was wrong; surfaces as 401.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
