package com.udhr.dto;

import com.udhr.model.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * One line of the audit trail, flattened for the administrator's review screen:
 * who did it, what they did, which record it touched, what changed, and when.
 *
 * A flat shape rather than the entity itself, so reviewing the trail never drags
 * a patient's full clinical record through the response — the administrator is
 * accountable for access, not entitled to the medical detail.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogEntryResponse {

    private Long id;
    private String staffName;
    private String staffNumber;
    private String staffRole;
    private String patientMrn;
    private String action;
    private String description;
    // What the value was before the change, and what it became. Null on actions
    // that only read a record — a view changes nothing.
    private String previousValue;
    private String newValue;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static AuditLogEntryResponse from(AuditLog log) {
        String staffName = log.getStaff() != null
                ? log.getStaff().getFirstName() + " " + log.getStaff().getLastName()
                : "Patient (self-service)";
        String staffNumber = log.getStaff() != null ? log.getStaff().getStaffNumber() : null;
        String staffRole = log.getStaff() != null ? log.getStaff().getRole() : "PATIENT";
        String patientMrn = log.getPatient() != null ? log.getPatient().getMrn() : null;

        return new AuditLogEntryResponse(
                log.getId(),
                staffName,
                staffNumber,
                staffRole,
                patientMrn,
                log.getAction(),
                log.getDescription(),
                log.getPreviousValue(),
                log.getNewValue(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
