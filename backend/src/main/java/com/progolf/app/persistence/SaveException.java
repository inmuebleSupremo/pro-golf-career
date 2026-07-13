package com.progolf.app.persistence;

/** Base type for save/load failures (spec: save-persistence). */
public class SaveException extends RuntimeException {
    public SaveException(String message) {
        super(message);
    }

    public SaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
