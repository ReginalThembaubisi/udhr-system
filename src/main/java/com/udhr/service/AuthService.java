package com.udhr.service;

import com.udhr.dto.ChangePasswordRequest;
import com.udhr.dto.LoginRequest;
import com.udhr.dto.LoginResponse;
import com.udhr.dto.SessionResponse;
import com.udhr.exception.LoginLockedException;
import com.udhr.model.AuditLog;
import com.udhr.model.RefreshToken;
import com.udhr.model.Staff;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

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

    @Autowired
    private LoginAttemptService loginAttemptService;

    // Lazy: PatientPortalService is a large service with its own dependencies,
    // and only refresh needs it.
    @Lazy
    @Autowired
    private PatientPortalService patientPortalService;

    private static final String INVALID_LOGIN = "Invalid staff number or password";
    // A real BCrypt hash of a random value, checked against when the staff
    // number doesn't exist so that case takes as long as a wrong password.
    private static final String UNKNOWN_ACCOUNT_HASH = new BCryptPasswordEncoder().encode(java.util.UUID.randomUUID().toString());

    /**
     * Signs a staff member in. Every wrong detail gets the same answer, so the
     * login can't be used to find out which staff numbers exist (fix list
     * P1-6), and 5 failures on an account, or 20 from one address, lock
     * further attempts for 15 minutes, even with the right password (P1-5).
     */
    public LoginResponse login(LoginRequest request, String clientAddress) {
        String accountKey = LoginAttemptService.accountKey(request.getStaffNumber());
        String addressKey = LoginAttemptService.addressKey(clientAddress);

        Instant lockedUntil = latest(loginAttemptService.lockedUntil(accountKey), loginAttemptService.lockedUntil(addressKey));
        if (lockedUntil != null) {
            throw new LoginLockedException("Too many failed sign-in attempts. Try again "
                    + LoginAttemptService.waitText(lockedUntil) + ".");
        }

        Staff staff = request.getStaffNumber() == null ? null
                : staffRepository.findByStaffNumber(request.getStaffNumber().trim()).orElse(null);
        // An unknown staff number still pays for a password check, so the
        // response time doesn't give away which numbers are real.
        String hash = staff != null ? staff.getPassword() : UNKNOWN_ACCOUNT_HASH;
        boolean passwordMatches = request.getPassword() != null && passwordEncoder.matches(request.getPassword(), hash);

        if (staff == null || !passwordMatches) {
            recordSecurityEvent(staff, "LOGIN_FAILED", staff == null
                    ? "Failed sign-in attempt for unknown staff number: " + request.getStaffNumber()
                    : "Failed sign-in attempt: incorrect password");
            if (loginAttemptService.recordFailure(accountKey, LoginAttemptService.MAX_ACCOUNT_FAILURES)) {
                recordSecurityEvent(staff, "LOGIN_LOCKED", "Sign-in locked for 15 minutes after "
                        + LoginAttemptService.MAX_ACCOUNT_FAILURES + " failed attempts on staff number " + request.getStaffNumber());
            }
            if (loginAttemptService.recordFailure(addressKey, LoginAttemptService.MAX_ADDRESS_FAILURES)) {
                recordSecurityEvent(null, "LOGIN_LOCKED", "Sign-in locked for 15 minutes for address " + clientAddress
                        + " after " + LoginAttemptService.MAX_ADDRESS_FAILURES + " failed attempts");
            }
            throw new RuntimeException(INVALID_LOGIN);
        }

        // Only someone who knows the password learns the account is inactive.
        if (staff.getActive() == null || !staff.getActive()) {
            recordSecurityEvent(staff, "LOGIN_BLOCKED", "Sign-in attempt on a deactivated account");
            throw new RuntimeException("Account is inactive");
        }

        loginAttemptService.reset(accountKey);
        recordSecurityEvent(staff, "LOGIN_SUCCESS", "Signed in");
        return buildSession(staff);
    }

    /**
     * Swaps a refresh token for a fresh access token, for a staff member or a
     * patient. The refresh token is rotated in the process, so each one works
     * exactly once.
     */
    public SessionResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("No refresh token supplied");
        }
        RefreshToken stored = tokenService.consumeRefreshToken(refreshToken);

        if (stored.getPatient() != null) {
            return patientPortalService.buildSession(stored.getPatient());
        }
        Staff staff = stored.getStaff();
        if (staff.getActive() == null || !staff.getActive()) {
            throw new RuntimeException("Account is inactive");
        }
        return buildSession(staff);
    }

    /**
     * Ends the current session for real: the access token goes on the revoked
     * list and the refresh token is revoked, so neither works again. Without
     * this, "logging out" only cleared the browser and the token stayed valid
     * for whoever else held a copy.
     */
    public void logout(String accessToken, String refreshToken, String staffNumber, String reason) {
        if (accessToken != null) {
            tokenService.revokeAccessToken(accessToken, reason);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenService.revokeRefreshToken(refreshToken);
        }
        if (staffNumber != null) {
            staffRepository.findByStaffNumber(staffNumber).ifPresent(staff -> recordSecurityEvent(staff, "LOGOUT",
                    "IDLE_TIMEOUT".equals(reason) ? "Signed out automatically after 12 minutes without activity" : "Signed out"));
        }
    }

    private static Instant latest(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
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
