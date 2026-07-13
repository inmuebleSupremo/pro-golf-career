package com.progolf.app.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Registration was attempted with a username that already exists (spec: authentication); surfaces as 409. */
@ResponseStatus(HttpStatus.CONFLICT)
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username already taken: " + username);
    }
}
