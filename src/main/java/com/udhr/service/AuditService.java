package com.udhr.service;

import com.udhr.model.AuditLog;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.StaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * One place to write the audit trail, so every service records the same shape:
 * who, what, which record, what changed, when, and from where.
 *
 * Before this existed each service built AuditLog objects by hand, which meant
 * coverage drifted — patient registration and dispensing were logged, diagnoses
 * and prescriptions were not. Centralising it also means the actor and IP
 * address are resolved the same way everywhere instead of being passed around.
 *
 * Audit writes never break the action being audited: a failure here is reported
 * to the console and swallowed. Losing a log line is bad; refusing to dispense
 * medication because a log line failed is worse.
 */
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private StaffRepository staffRepository;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AuditService.class);

    /** A meaningful, entity-linked event. Do not include patient notes, passwords or tokens. */
    public void recordEvent(Staff staff, Patient patient, String action, String description,
                            String entityType, Object entityId) {
        write(staff, patient, action, description, null, null, entityType,
                entityId == null ? null : String.valueOf(entityId));
    }

    /** A record was read. Nothing changed, so there are no before/after values. */
    public void recordView(Staff staff, Patient patient, String action, String description) {
        write(staff, patient, action, description, null, null);
    }

    /** A record was created, with a readable summary of what it now holds. */
    public void recordCreate(Staff staff, Patient patient, String action, String description, String newValue) {
        write(staff, patient, action, description, null, newValue);
    }

    /**
     * A record was changed. Both sides are captured so the trail answers "what
     * was changed", not just "something was changed".
     */
    public void recordChange(Staff staff, Patient patient, String action, String description,
                             String previousValue, String newValue) {
        write(staff, patient, action, description, previousValue, newValue);
    }

    /** An operational before/after change with a stable affected-record identifier. */
    public void recordChangeForEntity(Staff staff, Patient patient, String action, String description,
            String previousValue, String newValue, String entityType, Object entityId) {
        write(staff, patient, action, description, previousValue, newValue, entityType,
                entityId == null ? null : String.valueOf(entityId));
    }

    /** Record a rejected clinical or safety-sensitive attempt independently of a rolled-back operation. */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordRejectedAction(Staff staff, Patient patient, String action, String description,
                                     String entityType, Object entityId) {
        write(staff, patient, action, description, null, null, entityType,
                entityId == null ? null : String.valueOf(entityId));
    }

    /** Sign-ins, failures, lockouts, revocations — no patient involved. */
    public void recordSecurityEvent(Staff staff, String action, String description) {
        write(staff, null, action, description, null, null);
    }

    /**
     * Resolves the signed-in staff member from the security context, for the
     * services that don't already have the Staff object to hand.
     */
    public Staff currentStaff() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getPrincipal() == null) {
                return null;
            }
            return staffRepository.findByStaffNumber((String) authentication.getPrincipal()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void write(Staff staff, Patient patient, String action, String description,
                       String previousValue, String newValue) {
        write(staff, patient, action, description, previousValue, newValue, null, null);
    }

    private void write(Staff staff, Patient patient, String action, String description,
                       String previousValue, String newValue, String entityType, String entityId) {
        try {
            AuditLog log = new AuditLog();
            log.setStaff(staff);
            log.setPatient(patient);
            log.setAction(action);
            log.setActorType(staff != null ? "STAFF" : patient != null ? "PATIENT" : "ANONYMOUS");
            if (staff != null) {
                log.setActorLabel(AuditNarrative.readableRole(staff.getRole()) + " " + staff.getFirstName()
                        + " " + staff.getLastName() + " (" + staff.getStaffNumber() + ")");
                if (staff.getFacility() != null) log.setFacilityName(staff.getFacility().getName());
            } else if (patient != null) {
                log.setActorLabel("Patient " + patient.getMrn());
            }
            log.setOutcome(action != null && (action.contains("FAILED") || action.contains("LOCKED")
                    || action.contains("DENIED") || action.contains("BLOCKED")
                    || action.contains("REJECTED")) ? "FAILED" : "SUCCESS");
            log.setEntityType(entityType != null ? entityType : patient != null ? "PATIENT" : null);
            log.setEntityId(entityId != null ? entityId : patient != null ? String.valueOf(patient.getId()) : null);
            log.setDescription(description);
            log.setPreviousValue(previousValue);
            log.setNewValue(newValue);
            log.setIpAddress(currentIpAddress());
            auditLogRepository.save(log);
        } catch (Exception e) {
            LOG.error("AUDIT WRITE FAILED for action {}: {}", action, e.toString(), e);
        }
    }

    /**
     * The caller's address. Behind a load balancer the real client sits in
     * X-Forwarded-For, so that is preferred when present — the direct remote
     * address would otherwise just be the proxy on every single request.
     */
    private String currentIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            // Proxy headers can be spoofed unless a trusted proxy is configured.
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
