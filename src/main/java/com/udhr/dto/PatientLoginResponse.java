package com.udhr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientLoginResponse {
    private String token;
    private String idNumber;
    private String fullName;
    private String role;
}
