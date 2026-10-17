package com.udhr.config;

import com.udhr.model.Staff;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.service.AuditService;
import com.udhr.service.LoginAttemptService;
import com.udhr.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Last-resort recovery of a system administrator account that nobody inside
 * the system can rescue: locked out, password forgotten, phone lost, or taken
 * over by an attacker. Whoever can sign in to the Render dashboard sets
 *
 *   SYSADMIN_RECOVERY=SYSADMIN001          (the account to recover)
 *   SYSADMIN_INITIAL_PASSWORD=<temporary>  (at least 12 characters)
 *
 * and restarts. The account is reactivated with that temporary password (to be
 * changed at first sign-in), its authenticator app and lockout are cleared,
 * every session it has is ended, and the recovery goes in the audit trail.
 * Remove both variables afterwards.
 *
 * Access to Render is the final key on purpose: it already controls the code
 * and the database, so this grants nothing new. Each recovery runs once; the
 * same values on a later restart are skipped, so a forgotten variable can't
 * keep resetting the account.
 */
@Component
@Order(100) // after DataSeeder, which may switch off demo logins
public class EmergencyRecovery implements CommandLineRunner {

    private final StaffRepository staffRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${udhr.sysadmin-recovery:}")
    private String recoverStaffNumber;

    @Value("${udhr.sysadmin-initial-password:}")
    private String temporaryPassword;

    public EmergencyRecovery(StaffRepository staffRepository, AuditLogRepository auditLogRepository, AuditService auditService,
                             TokenService tokenService, LoginAttemptService loginAttemptService, BCryptPasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (recoverStaffNumber == null || recoverStaffNumber.isBlank()) {
            return;
        }
        if (temporaryPassword == null || temporaryPassword.length() < 12) {
            System.out.println("[EmergencyRecovery] Not run: set SYSADMIN_INITIAL_PASSWORD to a temporary password of at least 12 characters.");
            return;
        }
        Staff staff = staffRepository.findByStaffNumber(recoverStaffNumber.trim()).orElse(null);
        if (staff == null || !"SYSTEM_ADMIN".equals(staff.getRole())) {
            System.out.println("[EmergencyRecovery] Not run: " + recoverStaffNumber + " is not a system administrator account.");
            return;
        }

        // Same values as a recovery already done: skip, so a variable left in
        // place doesn't reset the account on every restart.
        String recoveryId = "recovery " + fingerprint(staff.getStaffNumber() + ":" + temporaryPassword);
        boolean alreadyDone = auditLogRepository.findByStaffIdOrderByCreatedAtDesc(staff.getId()).stream()
                .anyMatch(log -> "EMERGENCY_RECOVERY".equals(log.getAction()) && log.getDescription() != null
                        && log.getDescription().contains(recoveryId));
        if (alreadyDone) {
            System.out.println("[EmergencyRecovery] Already done for " + staff.getStaffNumber() + ". Remove SYSADMIN_RECOVERY and SYSADMIN_INITIAL_PASSWORD from Render.");
            return;
        }

        staff.setPassword(passwordEncoder.encode(temporaryPassword));
        staff.setMustChangePassword(true);
        staff.setActive(true);
        staff.setTotpSecret(null);
        staff.setTotpPendingSecret(null);
        staff.setTotpLastStep(null);
        staffRepository.save(staff);
        tokenService.revokeAllSessions(staff, "EMERGENCY_RECOVERY");
        loginAttemptService.reset(LoginAttemptService.accountKey(staff.getStaffNumber()));

        auditService.recordSecurityEvent(staff, "EMERGENCY_RECOVERY",
                "Account recovered from the server settings (" + recoveryId + "): temporary password set, "
                        + "two-step sign-in cleared, all sessions ended");
        System.out.println("[EmergencyRecovery] Recovered " + staff.getStaffNumber()
                + ". Sign in with the temporary password, then remove SYSADMIN_RECOVERY and SYSADMIN_INITIAL_PASSWORD from Render.");
    }

    // A short one-way fingerprint, so the audit trail can recognise a repeat
    // without storing the password.
    private static String fingerprint(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
