package com.progolf.app.persistence;

/** The destination save changed (or disappeared) since the active session last read or wrote it. */
public class SaveConflictException extends RuntimeException {

    public SaveConflictException(String saveId) {
        super("Career save has changed since this session opened: " + saveId);
    }
}
