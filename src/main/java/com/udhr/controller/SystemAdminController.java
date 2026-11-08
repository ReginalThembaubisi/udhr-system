package com.udhr.controller;

import com.udhr.dto.AuditLogEntryResponse;
import com.udhr.dto.PasswordResetResponse;
import com.udhr.model.Staff;
import com.udhr.security.JwtUtil;
import com.udhr.service.SystemAdminService;
import com.udhr.service.TwoFactorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The system administrator's endpoints. Locked to the SYSTEM_ADMIN role in
 * SecurityConfig — no clinical role and not the clinic's front desk reaches these.
 */
@RestController
@RequestMapping("/api/system-admin")
public class SystemAdminController {

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private com.udhr.service.SecurityAlertService securityAlertService;

    @Autowired
    private com.udhr.service.SecurityAlertMailer securityAlertMailer;

    @Autowired
    private com.udhr.service.AlertEmailSender alertEmailSender;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractStaffNumber(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractStaffNumber(authHeader.substring(7));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @GetMapping("/staff")
    public ResponseEntity<?> listStaff(HttpServletRequest request) {
        if (extractStaffNumber(request) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        return ResponseEntity.ok(systemAdminService.getAllStaff());
    }

    /** Resets a locked-out staff member's password and ends their sessions. */
    @PostMapping("/staff/{staffId}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long staffId, HttpServletRequest request) {
        String actor = extractStaffNumber(request);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        try {
            PasswordResetResponse response = systemAdminService.resetPassword(staffId, actor);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Clears someone's authenticator app (lost or new phone) after checking who
     * they are in person; they set it up again at their next sign-in.
     */
    @PostMapping("/staff/{staffId}/reset-two-factor")
    public ResponseEntity<?> resetTwoFactor(@PathVariable Long staffId, HttpServletRequest request) {
        String actor = extractStaffNumber(request);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        try {
            Staff staff = twoFactorService.resetFor(staffId, actor);
            return ResponseEntity.ok(Map.of("message", "Two-step sign-in reset for " + staff.getFirstName() + " " + staff.getLastName()
                    + ". They can set it up again after signing in."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Kills every session an account holds, right now. Used when a token is
     * believed stolen and hasn't expired on its own yet.
     */
    @PostMapping("/staff/{staffId}/force-logout")
    public ResponseEntity<?> forceLogout(@PathVariable Long staffId, HttpServletRequest request) {
        String actor = extractStaffNumber(request);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        try {
            systemAdminService.forceLogout(staffId, actor);
            return ResponseEntity.ok(Map.of("message", "All sessions for this account have been ended."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/staff/{staffId}/active")
    public ResponseEntity<?> setActive(@PathVariable Long staffId,
                                       @RequestBody Map<String, Boolean> body,
                                       HttpServletRequest request) {
        String actor = extractStaffNumber(request);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        try {
            boolean active = Boolean.TRUE.equals(body.get("active"));
            Staff updated = systemAdminService.setActive(staffId, active, actor);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /** Changes a staff account's email (where security alerts are sent, for system admins). */
    @PostMapping("/staff/{staffId}/email")
    public ResponseEntity<?> changeEmail(@PathVariable Long staffId,
                                         @RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        String actor = extractStaffNumber(request);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        try {
            return ResponseEntity.ok(systemAdminService.changeEmail(staffId, body.get("email"), actor));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /** Whether alert emails are set up, and who would get them. */
    @GetMapping("/alert-email")
    public ResponseEntity<?> alertEmailStatus() {
        return ResponseEntity.ok(Map.of(
                "configured", alertEmailSender.configured(),
                "recipients", securityAlertMailer.recipients()));
    }

    /** Sends a test email to the system admins, to check the setup works. */
    @PostMapping("/alert-email/test")
    public ResponseEntity<?> sendTestAlertEmail(HttpServletRequest request) {
        String actor = extractStaffNumber(request);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        if (!alertEmailSender.configured()) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Alert emails aren't set up yet: add BREVO_API_KEY and ALERT_EMAIL_FROM on Render."));
        }
        int sentTo = securityAlertMailer.sendTest(actor);
        if (sentTo == 0) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "The test email wasn't sent. Check the system admins have real emails, and that the sender is verified in Brevo."));
        }
        return ResponseEntity.ok(Map.of("message", "Test email sent to " + sentTo + " system admin" + (sentTo == 1 ? "" : "s") + "."));
    }

    /** Warnings worked out from the audit trail: unusual record access, sign-ins from new places, lockouts. */
    @GetMapping("/security-alerts")
    public ResponseEntity<?> securityAlerts(@RequestParam(required = false, defaultValue = "7") int days,
                                            HttpServletRequest request) {
        if (extractStaffNumber(request) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        return ResponseEntity.ok(securityAlertService.alerts(Math.max(1, Math.min(30, days))));
    }

    /** List registered patient portal accounts; excludes clinical and secret information. */
    @GetMapping("/patients")
    public ResponseEntity<?> listPatientAccounts(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            HttpServletRequest request) {
        if (extractStaffNumber(request) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        return ResponseEntity.ok(systemAdminService.getPatientAccounts(search, page, size));
    }

    /** The system-wide audit trail, newest first. */
    @GetMapping("/audit-log")
    public ResponseEntity<?> getAuditLog(@RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false, defaultValue = "200") int limit,
                                         @RequestParam(required = false, defaultValue = "0") int page,
                                         @RequestParam(required = false) String staffNumber,
                                         @RequestParam(required = false) String role,
                                         @RequestParam(required = false) String patientMrn,
                                         @RequestParam(required = false) String outcome,
                                         HttpServletRequest request) {
        String actor = extractStaffNumber(request);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        try {
            LocalDate start = parseDate(startDate);
            LocalDate end = parseDate(endDate);
            List<AuditLogEntryResponse> entries = systemAdminService.getAuditLog(start, end, action, staffNumber, role, patientMrn, outcome, limit, page, actor);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /** Everything that happened to one patient's record. */
    @GetMapping("/audit-log/patient/{patientId}")
    public ResponseEntity<?> getAuditLogForPatient(@PathVariable Long patientId, HttpServletRequest request) {
        String actor = extractStaffNumber(request);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        try {
            return ResponseEntity.ok(systemAdminService.getAuditLogForPatient(patientId, actor));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /** Everything one staff member did. */
    @GetMapping("/audit-log/staff/{staffId}")
    public ResponseEntity<?> getAuditLogForStaff(@PathVariable Long staffId, HttpServletRequest request) {
        String actor = extractStaffNumber(request);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        try {
            return ResponseEntity.ok(systemAdminService.getAuditLogForStaff(staffId, actor));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }
}
