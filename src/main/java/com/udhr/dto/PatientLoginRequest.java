package com.udhr.dto;

import lombok.Data;

@Data
public class PatientLoginRequest {
    private String idNumber;
    private String dateOfBirth; // Format: YYYY-MM-DD
    private String pin;         // portal PIN set at the clinic
}
