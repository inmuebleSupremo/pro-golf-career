package com.progolf.app.persistence;

/** No save exists for the given id (spec: save-persistence). */
public class SaveNotFoundException extends SaveException {
    public SaveNotFoundException(String saveId) {
        super("No save found for id: " + saveId);
    }
}
