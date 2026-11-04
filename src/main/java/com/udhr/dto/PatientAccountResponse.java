package com.udhr.dto;

import com.udhr.model.Patient;
import java.time.LocalDateTime;

/** Administrative account metadata only; does not expose ID numbers, PIN hashes or clinical details. */
public record PatientAccountResponse(
        Long id, String mrn, String fullName, LocalDateTime registeredAt,
        boolean portalPinSet, boolean mustChangePortalPin) {
    public static PatientAccountResponse from(Patient patient) {
        return new PatientAccountResponse(patient.getId(), patient.getMrn(),
                patient.getFirstName() + " " + patient.getLastName(), patient.getCreatedAt(),
                patient.isPortalPinSet(), Boolean.TRUE.equals(patient.getMustChangePortalPin()));
    }
}
