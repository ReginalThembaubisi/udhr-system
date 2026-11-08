package com.udhr.service;

import com.udhr.exception.LoginLockedException;
import com.udhr.model.Staff;
import com.udhr.repository.StaffRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * "Forgot your password?" for staff, without waiting for the system admin.
 *
 * The staff number alone is not enough (it's on name badges and printouts):
 * a 6-digit code is emailed to the work email on the account, and only with
 * that code can a new password be chosen.
 *
 * - The answer is always the same, so it can't reveal which staff numbers
 *   exist or what their email is.
 * - A code lasts 10 minutes, 5 wrong tries cancel it, only its hash is kept,
 *   one code a minute per account, requests per address are limited.
 * - Deactivated accounts get nothing.
 * - Resetting ends every session the account has, and two-step sign-in still
 *   applies: someone who gets into the email still needs the authenticator
 *   app for accounts that have it.
 * - With no email service (the demo), the email goes to the demo inbox.
 */
@Service
public class StaffPasswordResetService {

    static final int CODE_MINUTES = 10;
    static final int MAX_CODE_TRIES = 5;
    private static final int RESEND_SECONDS = 60;
    private static final int MAX_REQUESTS_PER_ADDRESS = 10;
    public static final String SENT_MESSAGE = "If that staff number belongs to an active account with an email address, "
            + "we've emailed a 6-digit code to it. It works for 10 minutes. No email? Your system administrator can reset "
            + "your password instead.";
    private static final String WRONG_CODE = "That code is wrong or has expired. Ask for a new code, or ask your system administrator.";

    private final StaffRepository staffRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AlertEmailSender email;
    private final AuditService auditService;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final SecureRandom random = new SecureRandom();

    public StaffPasswordResetService(StaffRepository staffRepository, BCryptPasswordEncoder passwordEncoder,
                                     AlertEmailSender email, AuditService auditService, TokenService tokenService,
                                     LoginAttemptService loginAttemptService) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.auditService = auditService;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
    }

    /** Step 1: email a code to the account's work email. Always answers the same way. */
    public String sendCode(String staffNumber, String clientAddress) {
        String addressKey = "password-reset:" + LoginAttemptService.addressKey(clientAddress);
        Instant locked = loginAttemptService.lockedUntil(addressKey);
        if (locked != null) {
            throw new LoginLockedException("Too many requests. Try again " + LoginAttemptService.waitText(locked)
                    + ", or ask your system administrator.");
        }
        if (loginAttemptService.recordFailure(addressKey, MAX_REQUESTS_PER_ADDRESS)) {
            auditService.recordSecurityEvent(null, "PASSWORD_RESET_LIMITED",
                    "Forgot-password requests paused for 15 minutes for address " + clientAddress);
        }

        Staff staff = find(staffNumber);
        if (staff == null || !Boolean.TRUE.equals(staff.getActive()) || !hasEmail(staff)) {
            return SENT_MESSAGE;
        }
        LocalDateTime now = LocalDateTime.now();
        if (staff.getPasswordResetSentAt() != null && staff.getPasswordResetSentAt().isAfter(now.minusSeconds(RESEND_SECONDS))) {
            return SENT_MESSAGE;
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        staff.setPasswordResetCodeHash(passwordEncoder.encode(code));
        staff.setPasswordResetExpiresAt(now.plusMinutes(CODE_MINUTES));
        staff.setPasswordResetAttempts(0);
        staff.setPasswordResetSentAt(now);
        staffRepository.save(staff);

        email.send(List.of(staff.getEmail().trim()), "UDHR: your password reset code",
                "Hello " + staff.getFirstName() + ",\n\n"
                        + "Your code to reset your UDHR password is " + code + ". It works for " + CODE_MINUTES + " minutes.\n\n"
                        + "Don't share it with anyone. If you didn't ask for this, tell your system administrator straight away: "
                        + "someone may be trying to get into your account.\n");
        auditService.recordSecurityEvent(staff, "PASSWORD_RESET_CODE_SENT", "asked for a password reset code by email");
        return SENT_MESSAGE;
    }

    /** Step 2: the code from the email and a new password. */
    public void resetPassword(String staffNumber, String code, String newPassword) {
        Staff staff = find(staffNumber);
        if (staff == null || !Boolean.TRUE.equals(staff.getActive()) || staff.getPasswordResetCodeHash() == null
                || staff.getPasswordResetExpiresAt() == null || staff.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(WRONG_CODE);
        }
        if (code == null || !passwordEncoder.matches(code.trim(), staff.getPasswordResetCodeHash())) {
            int tries = (staff.getPasswordResetAttempts() == null ? 0 : staff.getPasswordResetAttempts()) + 1;
            staff.setPasswordResetAttempts(tries);
            if (tries >= MAX_CODE_TRIES) {
                clearCode(staff);
            }
            staffRepository.save(staff);
            auditService.recordSecurityEvent(staff, "PASSWORD_RESET_FAILED", "entered a wrong password reset code");
            throw new RuntimeException(WRONG_CODE);
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Your new password must be at least 8 characters");
        }
        if (passwordEncoder.matches(newPassword, staff.getPassword())) {
            throw new RuntimeException("Choose a different password from your old one");
        }

        staff.setPassword(passwordEncoder.encode(newPassword));
        staff.setMustChangePassword(false);
        clearCode(staff);
        staffRepository.save(staff);
        loginAttemptService.reset(LoginAttemptService.accountKey(staff.getStaffNumber()));
        // Anyone signed in with the old password is signed out.
        tokenService.revokeAllSessions(staff, "PASSWORD_RESET_BY_EMAIL");
        auditService.recordSecurityEvent(staff, "PASSWORD_RESET_BY_EMAIL",
                "chose a new password using a code emailed to their work email; all sessions ended");
    }

    private Staff find(String staffNumber) {
        if (staffNumber == null || staffNumber.isBlank()) {
            return null;
        }
        return staffRepository.findByStaffNumber(staffNumber.trim()).orElse(null);
    }

    private static boolean hasEmail(Staff staff) {
        return staff.getEmail() != null && staff.getEmail().contains("@");
    }

    private static void clearCode(Staff staff) {
        staff.setPasswordResetCodeHash(null);
        staff.setPasswordResetExpiresAt(null);
        staff.setPasswordResetAttempts(0);
    }
}
