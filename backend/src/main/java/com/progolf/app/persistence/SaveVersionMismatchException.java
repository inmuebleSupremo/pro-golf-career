package com.progolf.app.persistence;

/** A save's format version is not supported by this store (spec: save-persistence). */
public class SaveVersionMismatchException extends SaveException {
    public SaveVersionMismatchException(String saveId, int found, int supported) {
        super("Save " + saveId + " has unsupported format version " + found + " (supported: " + supported + ")");
    }
}
