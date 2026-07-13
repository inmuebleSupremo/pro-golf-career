package com.progolf.app.persistence;

/** A save's payload does not match its recorded checksum — a corrupt or tampered file (spec: save-persistence). */
public class SaveCorruptedException extends SaveException {
    public SaveCorruptedException(String saveId) {
        super("Save is corrupt (checksum mismatch): " + saveId);
    }
}
