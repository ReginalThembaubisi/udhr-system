package com.udhr.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientLoginResponse implements SessionResponse {
    private String token;
    // Lets a page reload keep the patient signed in now that the access token
    // lives only in memory. Sent as an HttpOnly cookie, never in the body.
    @JsonIgnore
    private String refreshToken;
    private String idNumber;
    private String fullName;
    private String role;
    private Boolean mustChangePin;
    // New device: sign-in isn't finished until the code texted to the patient's
    // cellphone is entered with this challenge.
    private Boolean deviceCodeRequired;
    private String challengeToken;
    private String codeSentTo;

    public PatientLoginResponse(String token, String refreshToken, String idNumber, String fullName, String role) {
        this(token, refreshToken, idNumber, fullName, role, false);
    }

    public PatientLoginResponse(String token, String refreshToken, String idNumber, String fullName, String role, Boolean mustChangePin) {
        this(token, refreshToken, idNumber, fullName, role, mustChangePin, null, null, null);
    }
}
