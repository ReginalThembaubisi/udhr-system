package com.udhr.dto;

import com.udhr.model.AuditLog;
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

    private String title;
    private String summary;
    private String outcome;
    private String actorType;
    private String entityType;
    private String entityId;
    private String facilityName;

    public static AuditLogEntryResponse from(AuditLog log) {
        String staffName = log.getStaff() != null
                ? log.getStaff().getFirstName() + " " + log.getStaff().getLastName()
                : log.getPatient() != null ? "Patient (self-service)" : "Unidentified visitor";
        String staffNumber = log.getStaff() != null ? log.getStaff().getStaffNumber() : null;
        String staffRole = log.getStaff() != null ? log.getStaff().getRole() : log.getPatient() != null ? "PATIENT" : "ANONYMOUS";
        String patientMrn = log.getPatient() != null ? log.getPatient().getMrn() : null;

        AuditLogEntryResponse response = new AuditLogEntryResponse();
        response.setId(log.getId());
        response.setStaffName(staffName);
        response.setStaffNumber(staffNumber);
        response.setStaffRole(staffRole);
        response.setPatientMrn(patientMrn);
        response.setAction(log.getAction());
        // Older clinical records sometimes contain free-text medical notes in before/after.
        // Never send those snapshots to the general SYSTEM_ADMIN audit view.
        boolean operational = log.getAction() != null && (log.getAction().startsWith("STOCK_")
                || log.getAction().startsWith("ADMIN_") || log.getAction().startsWith("QUEUE_"));
        response.setPreviousValue(operational ? log.getPreviousValue() : null);
        response.setNewValue(operational ? log.getNewValue() : null);
        response.setDescription(com.udhr.service.AuditNarrative.summary(log));
        response.setIpAddress(log.getIpAddress());
        response.setCreatedAt(log.getCreatedAt());
        response.setTitle(com.udhr.service.AuditNarrative.title(log.getAction()));
        response.setSummary(com.udhr.service.AuditNarrative.summary(log));
        response.setOutcome(log.getOutcome() != null ? log.getOutcome() :
                log.getAction() != null && (log.getAction().contains("FAILED") || log.getAction().contains("DENIED")
                        || log.getAction().contains("LOCKED") || log.getAction().contains("BLOCKED") || log.getAction().contains("REJECTED")) ? "FAILED" : "SUCCESS");
        response.setActorType(log.getActorType() != null ? log.getActorType()
                : log.getStaff() != null ? "STAFF" : log.getPatient() != null ? "PATIENT" : "ANONYMOUS");
        response.setEntityType(log.getEntityType());
        response.setEntityId(log.getEntityId());
        response.setFacilityName(log.getFacilityName());
        return response;
    }
}
