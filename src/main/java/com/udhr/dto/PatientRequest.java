package com.udhr.dto;

import lombok.Data;

@Data
public class PatientRequest {
    private String idNumber;
    private String firstName;
    private String lastName;
    private String dateOfBirth; // Will be parsed to LocalDate
    private String gender;
    private String contactNumber;
    private String address;
}
