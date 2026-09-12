package com.udhr.security;

import com.udhr.model.Patient;
import com.udhr.model.Staff;
import org.springframework.security.access.AccessDeniedException;

/**
 * Enforces that a DOCTOR/NURSE can only read or write records for patients
 * registered at their own facility. ADMIN staff operate across facilities.
 */
public final class FacilityGuard {

    private FacilityGuard() {
    }

    public static void assertSameFacility(Staff staff, Patient patient) {
        if (staff.getRole() != null && "ADMIN".equalsIgnoreCase(staff.getRole())) {
            return;
        }

        Long staffFacilityId = staff.getFacility() != null ? staff.getFacility().getId() : null;
        Long patientFacilityId = patient.getFacility() != null ? patient.getFacility().getId() : null;

        if (staffFacilityId == null || patientFacilityId == null || !staffFacilityId.equals(patientFacilityId)) {
            throw new AccessDeniedException("This patient is not registered at your facility.");
        }
    }
}
