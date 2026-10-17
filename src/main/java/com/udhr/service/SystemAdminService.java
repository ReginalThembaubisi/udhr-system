package com.udhr.service;

import com.udhr.dto.AuditLogEntryResponse;
import com.udhr.dto.PasswordResetResponse;
import com.udhr.model.AuditLog;
import com.udhr.model.Staff;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * What the system administrator can do.
 *
 * This role sits above the clinical roles and above the clinic's front desk. It
 * exists because a system holding patient, staff and doctor records needs
 * someone who can answer "who has access, who did what, and how do we shut
 * someone out" — none of which belongs to a nurse, a doctor or a receptionist.
 *
 * It is deliberately an oversight role, not a clinical one: nothing here reads
 * or writes a patient's medical record. The administrator manages accounts and
 * reviews the audit trail; they do not diagnose, prescribe or dispense.
 */
@Service
public class SystemAdminService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    // ---- Account management ------------------------------------------------------

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    /**
     * Resets a staff member's password — the doctor who forgot theirs, or the
     * front-desk clerk who can't sign in.
     *
     * A random temporary password is generated and returned once, for the
     * administrator to hand over. It is never stored in readable form, and the
     * account is flagged to force a change at next sign-in, so the administrator
     * does not end up knowing anyone's working password.
     *
     * Resetting also ends every session the account currently has: if the reset
     * is happening because the account was compromised, leaving the old tokens
     * alive would defeat the point.
     */
    @Transactional
    public PasswordResetResponse resetPassword(Long staffId, String performedByStaffNumber) {
        Staff target = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff member not found"));
        Staff performedBy = resolveActor(performedByStaffNumber);

        String temporaryPassword = generateTemporaryPassword();
        target.setPassword(passwordEncoder.encode(temporaryPassword));
        target.setMustChangePassword(true);
        staffRepository.save(target);

        tokenService.revokeAllSessions(target, "ADMIN_PASSWORD_RESET");
        // Lift any lockout too, so the person can sign in with the new
        // password straight away instead of waiting out the 15 minutes.
        loginAttemptService.reset(LoginAttemptService.accountKey(target.getStaffNumber()));

        record(performedBy, "ADMIN_PASSWORD_RESET",
                "Reset password for " + describe(target) + " and ended all their sessions");

        return new PasswordResetResponse(
                target.getStaffNumber(),
                target.getFirstName() + " " + target.getLastName(),
                temporaryPassword,
                "Give this temporary password to the staff member. They must change it at next sign-in."
        );
    }

    /**
     * Ends every session an account has, immediately.
     *
     * This is the answer to a stolen token that hasn't expired yet: the
     * administrator forces the account out and the stolen token stops working on
     * its very next request, rather than staying valid until it expires.
     */
    @Transactional
    public void forceLogout(Long staffId, String performedByStaffNumber) {
        Staff target = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff member not found"));
        Staff performedBy = resolveActor(performedByStaffNumber);

        tokenService.revokeAllSessions(target, "ADMIN_FORCE_LOGOUT");
        record(performedBy, "ADMIN_FORCE_LOGOUT", "Ended all sessions for " + describe(target));
    }

    /**
     * Turns an account off without deleting it, so the person's history in the
     * audit trail stays intact and attributable.
     */
    @Transactional
    public Staff setActive(Long staffId, boolean active, String performedByStaffNumber) {
        Staff target = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff member not found"));
        Staff performedBy = resolveActor(performedByStaffNumber);
        if (!active) {
            AdminSafeguards.checkCanDeactivate(target, performedBy, staffRepository);
        }

        target.setActive(active);
        staffRepository.save(target);

        if (!active) {
            // A deactivated account must not keep working off an existing token.
            tokenService.revokeAllSessions(target, "ADMIN_DEACTIVATED");
        }

        record(performedBy, active ? "ADMIN_ACTIVATED_ACCOUNT" : "ADMIN_DEACTIVATED_ACCOUNT",
                (active ? "Reactivated " : "Deactivated ") + describe(target));
        return target;
    }

    // ---- Audit trail -------------------------------------------------------------

    /**
     * Everything that happened to one patient's record: who viewed it, who
     * changed it, what changed and when.
     */
    public List<AuditLogEntryResponse> getAuditLogForPatient(Long patientId, String performedByStaffNumber) {
        Staff performedBy = resolveActor(performedByStaffNumber);
        record(performedBy, "ADMIN_VIEW_AUDIT_LOG", "Reviewed the audit trail for patient #" + patientId);
        return toResponses(auditLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId));
    }

    /** Everything one staff member did — "Dr XYZ did this, then this". */
    public List<AuditLogEntryResponse> getAuditLogForStaff(Long staffId, String performedByStaffNumber) {
        Staff performedBy = resolveActor(performedByStaffNumber);
        record(performedBy, "ADMIN_VIEW_AUDIT_LOG", "Reviewed the audit trail for staff #" + staffId);
        return toResponses(auditLogRepository.findByStaffIdOrderByCreatedAtDesc(staffId));
    }

    /**
     * The whole trail, newest first, optionally narrowed to a date range and an
     * action type — the view used when investigating an incident.
     */
    public List<AuditLogEntryResponse> getAuditLog(LocalDate startDate, LocalDate endDate, String action,
                                                   int limit, String performedByStaffNumber) {
        Staff performedBy = resolveActor(performedByStaffNumber);
        record(performedBy, "ADMIN_VIEW_AUDIT_LOG", "Reviewed the system audit trail");

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        return toResponses(auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(log -> start == null || !log.getCreatedAt().isBefore(start))
                .filter(log -> end == null || log.getCreatedAt().isBefore(end))
                .filter(log -> action == null || action.isBlank()
                        || action.equalsIgnoreCase(log.getAction()))
                .limit(limit > 0 ? limit : 200)
                .collect(Collectors.toList()));
    }

    // ---- Helpers -----------------------------------------------------------------

    private List<AuditLogEntryResponse> toResponses(List<AuditLog> logs) {
        return logs.stream().map(AuditLogEntryResponse::from).collect(Collectors.toList());
    }

    private Staff resolveActor(String staffNumber) {
        return staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Acting staff member not found"));
    }

    private String describe(Staff staff) {
        return staff.getFirstName() + " " + staff.getLastName() + " (" + staff.getStaffNumber() + ", " + staff.getRole() + ")";
    }

    // The administrator's own actions are audited too — an oversight role that
    // isn't itself accountable is a gap, not a control.
    private void record(Staff performedBy, String action, String description) {
        AuditLog log = new AuditLog();
        log.setStaff(performedBy);
        log.setAction(action);
        log.setDescription(description);
        auditLogRepository.save(log);
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(TEMP_PASSWORD_ALPHABET.charAt(RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length())));
        }
        return builder.toString();
    }
}
