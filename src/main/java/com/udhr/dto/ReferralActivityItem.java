package com.udhr.dto;

import com.udhr.model.Referral;
import java.time.LocalDateTime;

/**
 * One row of the referral report's recent activity, as the front desk sees it.
 *
 * The report is open to the front desk (ADMIN), which is not clinical staff, so
 * this carries who went where and how urgently, and deliberately leaves out the
 * clinical summary and the receiving clinician's response notes. Returning the
 * Referral entity itself used to send both, along with the patient's full record.
 * The nested shapes match what the admin screen already reads (patient.firstName,
 * fromFacility.name, ...).
 */
public record ReferralActivityItem(
        Long id,
        PatientName patient,
        FacilityName fromFacility,
        FacilityName toFacility,
        Referral.Urgency urgency,
        Referral.Status status,
        String reason,
        LocalDateTime referredAt
) {
    public record PatientName(String firstName, String lastName) {}

    public record FacilityName(String name) {}

    public static ReferralActivityItem from(Referral r) {
        return new ReferralActivityItem(
                r.getId(),
                new PatientName(r.getPatient().getFirstName(), r.getPatient().getLastName()),
                new FacilityName(r.getFromFacility().getName()),
                new FacilityName(r.getToFacility().getName()),
                r.getUrgency(),
                r.getStatus(),
                r.getReason(),
                r.getReferredAt()
        );
    }
}
