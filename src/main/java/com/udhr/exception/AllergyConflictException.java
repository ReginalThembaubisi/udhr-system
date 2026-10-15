package com.udhr.exception;

import java.util.List;

/**
 * Thrown when a medication conflicts with the patient's recorded allergies and
 * the prescriber hasn't given a reason to go ahead anyway. Carries the warnings
 * so the screen can show the prescriber exactly what was found.
 */
public class AllergyConflictException extends RuntimeException {

    private final List<String> conflicts;

    public AllergyConflictException(List<String> conflicts) {
        super("Allergy warning: " + String.join("; ", conflicts));
        this.conflicts = List.copyOf(conflicts);
    }

    public List<String> getConflicts() {
        return conflicts;
    }
}
