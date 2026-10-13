package com.udhr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    // Exchanged for a new access token when the short-lived one runs out, so
    // the 30-minute access token doesn't mean signing in every half hour.
    private String refreshToken;
    private String staffNumber;
    private String fullName;
    private String role;
    private Long facilityId;
    private Boolean mustChangePassword;
}
