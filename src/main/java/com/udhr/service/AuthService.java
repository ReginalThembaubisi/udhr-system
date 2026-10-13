package com.udhr.service;

import com.udhr.dto.ChangePasswordRequest;
import com.udhr.dto.LoginRequest;
import com.udhr.dto.LoginResponse;
import com.udhr.model.AuditLog;
import com.udhr.model.Staff;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public LoginResponse login(LoginRequest request) {
        Staff staff = staffRepository.findByStaffNumber(request.getStaffNumber())
                .orElseThrow(() -> {
                    // Logged without a staff record attached — someone is trying
                    // staff numbers that don't exist, which is worth seeing.
                    recordSecurityEvent(null, "LOGIN_FAILED",
                            "Failed sign-in attempt for unknown staff number: " + request.getStaffNumber());
                    return new RuntimeException("Staff not found");
                });

        if (staff.getActive() == null || !staff.getActive()) {
            recordSecurityEvent(staff, "LOGIN_BLOCKED", "Sign-in attempt on a deactivated account");
            throw new RuntimeException("Account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            recordSecurityEvent(staff, "LOGIN_FAILED", "Failed sign-in attempt: incorrect password");
            throw new RuntimeException("Invalid password");
        }

        recordSecurityEvent(staff, "LOGIN_SUCCESS", "Signed in");
        return buildSession(staff);
    }

    /**
     * Swaps a refresh token for a fresh access token. The refresh token is
     * rotated in the process, so each one works exactly once.
     */
    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("No refresh token supplied");
        }
        Staff staff = tokenService.consumeRefreshToken(refreshToken);

        if (staff.getActive() == null || !staff.getActive()) {
            throw new RuntimeException("Account is inactive");
        }
        return buildSession(staff);
    }

    /**
     * Ends the current session for real: the access token goes on the revoked
     * list and the refresh token is consumed, so neither works again. Without
     * this, "logging out" only cleared the browser and the token stayed valid
     * for whoever else held a copy.
     */
    public void logout(String accessToken, String refreshToken, String staffNumber) {
        if (accessToken != null) {
            tokenService.revokeAccessToken(accessToken, "LOGOUT");
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                tokenService.consumeRefreshToken(refreshToken);
            } catch (Exception ignored) {
                // Already used or unknown — nothing left to revoke.
            }
        }
        staffRepository.findByStaffNumber(staffNumber)
                .ifPresent(staff -> recordSecurityEvent(staff, "LOGOUT", "Signed out"));
    }

    // Returns a freshly issued token (mustChangePassword cleared) because the
    // old token's claim is baked in and immutable — the caller must swap to
    // this one or every subsequent request will keep getting blocked by the
    // filter even though the password was actually changed.
    public LoginResponse changePassword(String staffNumber, ChangePasswordRequest request) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), staff.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters");
        }

        staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
        staff.setMustChangePassword(false);
        staffRepository.save(staff);

        // Changing a password is how someone reacts to a suspected compromise,
        // so it must also end every session the account already has — otherwise
        // a stolen token keeps working against the new password.
        tokenService.revokeAllSessions(staff, "PASSWORD_CHANGE");
        recordSecurityEvent(staff, "PASSWORD_CHANGED", "Password changed; all other sessions ended");

        return buildSession(staff);
    }

    private LoginResponse buildSession(Staff staff) {
        boolean mustChangePassword = staff.getMustChangePassword() != null && staff.getMustChangePassword();
        String token = jwtUtil.generateToken(staff.getStaffNumber(), staff.getRole(), mustChangePassword);
        String refreshToken = tokenService.issueRefreshToken(staff);
        String fullName = staff.getFirstName() + " " + staff.getLastName();
        Long facilityId = staff.getFacility() != null ? staff.getFacility().getId() : null;

        return new LoginResponse(
                token,
                refreshToken,
                staff.getStaffNumber(),
                fullName,
                staff.getRole(),
                facilityId,
                mustChangePassword
        );
    }

    // Security events belong in the same audit trail as clinical actions, so a
    // system administrator reviewing an incident sees sign-ins, failures and
    // record access in one place rather than three.
    private void recordSecurityEvent(Staff staff, String action, String description) {
        try {
            AuditLog log = new AuditLog();
            log.setStaff(staff);
            log.setAction(action);
            log.setDescription(description);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Never let audit logging block a sign-in.
            System.out.println("[AuthService] Could not write audit entry: " + e.getMessage());
        }
    }
}
