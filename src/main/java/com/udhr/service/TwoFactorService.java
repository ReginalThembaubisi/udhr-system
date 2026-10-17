package com.udhr.service;

import com.udhr.dto.LoginResponse;
import com.udhr.model.Staff;
import com.udhr.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Setting up, turning off and resetting two-step sign-in for staff. The sign-in
 * check itself is in AuthService.verifyTwoFactor.
 */
@Service
public class TwoFactorService {

    @Autowired private StaffRepository staffRepository;
    @Autowired private TotpService totpService;
    @Autowired private TokenService tokenService;
    @Autowired private AuditService auditService;
    @Autowired private AuthService authService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Value("${udhr.staff-2fa:optional}")
    private String staffTwoFactor;

    @Value("${udhr.sysadmin-2fa:required}")
    private String sysadminTwoFactor;

    public Map<String, Object> status(String staffNumber) {
        Staff staff = find(staffNumber);
        return Map.of("enabled", staff.isTwoFactorEnabled(), "required", isRequiredFor(staff));
    }

    /**
     * Compulsory for everyone with STAFF_2FA=required, and for system
     * administrators unless SYSADMIN_2FA=optional: the account that can reset
     * everyone else's must not fall to a password alone.
     */
    public boolean isRequiredFor(Staff staff) {
        return "required".equalsIgnoreCase(staffTwoFactor)
                || ("SYSTEM_ADMIN".equals(staff.getRole()) && !"optional".equalsIgnoreCase(sysadminTwoFactor));
    }

    /** Step 1: a new secret, shown once as a QR code (and as text to type in). */
    public Map<String, String> begin(String staffNumber) {
        Staff staff = find(staffNumber);
        String secret = totpService.newSecret();
        staff.setTotpPendingSecret(totpService.encrypt(secret));
        staffRepository.save(staff);
        return Map.of("secret", secret, "otpauthUri", totpService.otpauthUri(staff.getStaffNumber(), secret));
    }

    /**
     * Step 2: the first code from the app proves it was set up correctly. Every
     * other session the account has is ended, in case someone else was already
     * signed in, and a fresh session is handed back.
     */
    public LoginResponse confirm(String staffNumber, String code, String currentAccessToken) {
        Staff staff = find(staffNumber);
        String pending = totpService.decrypt(staff.getTotpPendingSecret());
        long step = totpService.verify(pending, code, null);
        if (pending == null || step < 0) {
            throw new RuntimeException("That code isn't right. Make sure the app shows UDHR and enter the current code.");
        }
        staff.setTotpSecret(staff.getTotpPendingSecret());
        staff.setTotpPendingSecret(null);
        staff.setTotpLastStep(step);
        staffRepository.save(staff);
        tokenService.revokeAllSessions(staff, "TWO_FACTOR_ENABLED");
        // revokeAllSessions works to the second, so the token used for this
        // request (often issued the same second) is revoked by name as well.
        if (currentAccessToken != null) {
            tokenService.revokeAccessToken(currentAccessToken, "TWO_FACTOR_ENABLED");
        }
        auditService.recordSecurityEvent(staff, "TWO_FACTOR_ENABLED", "Turned on two-step sign-in; other sessions ended");
        return authService.buildSession(staff);
    }

    /** Turning it off needs the password and a current code, and isn't allowed when it is compulsory. */
    public void disable(String staffNumber, String password, String code) {
        Staff staff = find(staffNumber);
        if (isRequiredFor(staff)) {
            throw new RuntimeException("Two-step sign-in is required for your account and can't be turned off.");
        }
        if (password == null || !passwordEncoder.matches(password, staff.getPassword())) {
            throw new RuntimeException("Password is incorrect");
        }
        long step = totpService.verify(totpService.decrypt(staff.getTotpSecret()), code, staff.getTotpLastStep());
        if (step < 0) {
            throw new RuntimeException("That code isn't right.");
        }
        clear(staff);
        auditService.recordSecurityEvent(staff, "TWO_FACTOR_DISABLED", "Turned off two-step sign-in");
    }

    /**
     * A system administrator clearing it for someone who lost their phone,
     * after checking who they are in person. Their sessions are ended too.
     */
    public Staff resetFor(Long staffId, String performedBy) {
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new RuntimeException("Staff not found"));
        // Someone signed in as you shouldn't be able to strip your second step.
        // A system admin's own reset goes through another system admin, or the
        // emergency recovery on Render (EmergencyRecovery).
        if (staff.getStaffNumber().equalsIgnoreCase(performedBy)) {
            throw new RuntimeException("You can't reset your own two-step sign-in. Ask another system administrator.");
        }
        clear(staff);
        tokenService.revokeAllSessions(staff, "TWO_FACTOR_RESET");
        auditService.recordSecurityEvent(staffRepository.findByStaffNumber(performedBy).orElse(null), "TWO_FACTOR_RESET",
                "Reset two-step sign-in for " + staff.getFirstName() + " " + staff.getLastName() + " (" + staff.getStaffNumber() + ")");
        return staff;
    }

    private void clear(Staff staff) {
        staff.setTotpSecret(null);
        staff.setTotpPendingSecret(null);
        staff.setTotpLastStep(null);
        staffRepository.save(staff);
    }

    private Staff find(String staffNumber) {
        return staffRepository.findByStaffNumber(staffNumber).orElseThrow(() -> new RuntimeException("Staff not found"));
    }
}
