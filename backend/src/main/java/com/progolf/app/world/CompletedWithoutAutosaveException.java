package com.progolf.app.world;

/** Gameplay completed, but its automatic recovery checkpoint was not written. Retrying gameplay is unsafe. */
public class CompletedWithoutAutosaveException extends RuntimeException {
    public CompletedWithoutAutosaveException(String action, Throwable cause) {
        super(action + " completed, but its automatic recovery checkpoint was not saved", cause);
    }
}
