package com.progolf.app.world;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a world session id is not known; surfaces as HTTP 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class WorldSessionNotFoundException extends RuntimeException {

    public WorldSessionNotFoundException(String id) {
        super("No world session with id: " + id);
    }
}
