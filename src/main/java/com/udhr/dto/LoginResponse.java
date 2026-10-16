package com.udhr.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginResponse implements SessionResponse {
    private String token;
    // Exchanged for a new access token when the short-lived one runs out.
    // Sent as an HttpOnly cookie, never in the body (fix list P1-3).
    @JsonIgnore
    private String refreshToken;
    private String staffNumber;
    private String fullName;
    private String role;
    private Long facilityId;
    private Boolean mustChangePassword;

    // Two-step sign-in: the password was right, now the authenticator code is
    // needed. No session is issued until then; challengeToken carries the
    // proof of the password to /api/auth/login/verify.
    private Boolean twoFactorRequired;
    private String challengeToken;
    // STAFF_2FA=required and this account has no authenticator app yet.
    private Boolean twoFactorSetupRequired;

    // Shown after signing in, so someone else using the account gets noticed:
    // when and where the previous sign-in was, and failed attempts since.
    private LocalDateTime lastSignInAt;
    private String lastSignInFrom;
    private Long failedAttemptsSinceLastSignIn;

    public LoginResponse(String token, String refreshToken, String staffNumber, String fullName,
                         String role, Long facilityId, Boolean mustChangePassword) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.staffNumber = staffNumber;
        this.fullName = fullName;
        this.role = role;
        this.facilityId = facilityId;
        this.mustChangePassword = mustChangePassword;
    }
}
