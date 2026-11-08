package com.udhr.service;

import com.udhr.dto.AuditLogEntryResponse;
import com.udhr.dto.PasswordResetResponse;
import com.udhr.dto.PatientAccountResponse;
import com.udhr.model.Patient;
import com.udhr.repository.PatientRepository;
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
    private PatientRepository patientRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    // ---- Account management ------------------------------------------------------

    public List<Staff> getAllStaff() {
        auditService.recordSecurityEvent(auditService.currentStaff(), "ADMIN_VIEW_STAFF_LIST",
                "reviewed the staff account directory");
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

    /**
     * Changes the email on a staff account: security alerts go to the system
     * admins' emails, so theirs must be a real inbox someone reads.
     */
    public Staff changeEmail(Long staffId, String email, String performedByStaffNumber) {
        Staff target = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff member not found"));
        Staff performedBy = resolveActor(performedByStaffNumber);
        String cleaned = email == null ? "" : email.trim().toLowerCase();
        if (!cleaned.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || cleaned.length() > 120) {
            throw new RuntimeException("Enter a valid email address");
        }
        boolean taken = staffRepository.findAll().stream()
                .anyMatch(s -> !s.getId().equals(target.getId()) && cleaned.equalsIgnoreCase(s.getEmail()));
        if (taken) {
            throw new RuntimeException("Another account already uses that email");
        }
        String previous = target.getEmail();
        target.setEmail(cleaned);
        staffRepository.save(target);
        record(performedBy, "ADMIN_CHANGED_EMAIL", "Changed the email of " + describe(target)
                + " from " + previous + " to " + cleaned);
        return target;
    }

    /**
     * Paginated patient portal accounts for access oversight only. This is NOT
     * a clinical record endpoint: DOB, ID numbers, contact details, PIN hashes
     * and any medical data are deliberately excluded from the response.
     */
    public org.springframework.data.domain.Page<PatientAccountResponse> getPatientAccounts(
            String search, int page, int size) {
        String term = search == null ? "" : search.trim().toLowerCase(java.util.Locale.ROOT);
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page),
                Math.max(1, Math.min(size, 50)),
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id")));
        org.springframework.data.jpa.domain.Specification<Patient> filter = (root, query, cb) -> {
            if (term.isBlank()) return cb.conjunction();
            String pattern = "%" + term.replace("%", "\\%").replace("_", "\\_") + "%";
            // Escape wildcard characters so a search cannot accidentally list everything.
            return cb.or(cb.like(cb.lower(root.get("mrn")), pattern, '\\'),
                    cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("lastName")), pattern, '\\'));
        };
        auditService.recordSecurityEvent(auditService.currentStaff(), "ADMIN_VIEW_PATIENT_ACCOUNTS",
                "reviewed registered patient portal accounts" + (term.isBlank() ? "" : " using a search"));
        return patientRepository.findAll(filter, pageable).map(PatientAccountResponse::from);
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

        return searchAudit(start, end, action, null, null, null, null, limit, 0);
    }

    /** Search by actual event attributes using database pagination, not a full-table scan. */
    public List<AuditLogEntryResponse> getAuditLog(LocalDate startDate, LocalDate endDate, String activity,
            String staffNumber, String role, String patientMrn, String outcome, int limit, int page,
            String performedByStaffNumber) {
        Staff performedBy = resolveActor(performedByStaffNumber);
        record(performedBy, "ADMIN_VIEW_AUDIT_LOG", "reviewed audit history with search filters");
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        return searchAudit(start, end, activity, staffNumber, role, patientMrn, outcome, limit, page);
    }

    private List<AuditLogEntryResponse> searchAudit(LocalDateTime start, LocalDateTime end,
            String activity, String staffNumber, String role, String patientMrn, String outcome,
            int limit, int page) {
        org.springframework.data.jpa.domain.Specification<AuditLog> criteria = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            if (end != null) predicates.add(cb.lessThan(root.get("createdAt"), end));
            if (activity != null && !activity.isBlank()) {
                String term = "%" + activity.trim().toLowerCase(java.util.Locale.ROOT) + "%";
                String codeTerm = "%" + activity.trim().replace(' ', '_').toLowerCase(java.util.Locale.ROOT) + "%";
                var matchingCodes = AuditNarrative.matchingCodes(activity.trim());
                predicates.add(cb.or(cb.like(cb.lower(root.get("action")), codeTerm),
                        cb.like(cb.lower(root.get("description")), term),
                        root.get("action").in(matchingCodes)));
            }
            if (staffNumber != null && !staffNumber.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.join("staff").get("staffNumber")),
                        staffNumber.trim().toLowerCase(java.util.Locale.ROOT)));
            }
            if (role != null && !role.isBlank()) {
                String selectedRole = role.trim().toUpperCase(java.util.Locale.ROOT);
                if ("PATIENT".equals(selectedRole)) {
                    // Patient portal events do not have a staff member to join to.
                    predicates.add(cb.equal(root.get("actorType"), "PATIENT"));
                } else {
                    predicates.add(cb.equal(root.join("staff").get("role"), selectedRole));
                }
            }
            if (patientMrn != null && !patientMrn.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.join("patient").get("mrn")),
                        patientMrn.trim().toLowerCase(java.util.Locale.ROOT)));
            }
            if (outcome != null && !outcome.isBlank()) {
                predicates.add(cb.equal(root.get("outcome"), outcome.trim().toUpperCase(java.util.Locale.ROOT)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page),
                Math.max(1, Math.min(limit <= 0 ? 200 : limit, 200)),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id")));
        return toResponses(auditLogRepository.findAll(criteria, pageable).getContent());
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
        auditService.recordSecurityEvent(performedBy, action, description);
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(TEMP_PASSWORD_ALPHABET.charAt(RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length())));
        }
        return builder.toString();
    }
}
