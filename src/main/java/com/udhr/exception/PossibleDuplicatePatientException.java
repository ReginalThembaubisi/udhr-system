package com.udhr.exception;

import com.udhr.model.Patient;

/**
 * Registration found an existing patient with the same name and date of birth
 * (but no matching ID or passport number, which would be a hard stop). The
 * front desk is shown the existing record and can register anyway if it really
 * is a different person.
 */
public class PossibleDuplicatePatientException extends RuntimeException {

    private final Patient existing;

    public PossibleDuplicatePatientException(Patient existing) {
        super("A patient named " + existing.getFirstName() + " " + existing.getLastName()
                + ", born " + existing.getDateOfBirth() + ", is already registered (" + existing.getMrn() + ").");
        this.existing = existing;
    }

    public Patient getExisting() {
        return existing;
    }
}
