package com.udhr.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
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
}
