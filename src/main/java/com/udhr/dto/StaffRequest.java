package com.udhr.dto;

import lombok.Data;

@Data
public class StaffRequest {
    private String staffNumber;
    private String firstName;
    private String lastName;
    private String role; // "ADMIN", "DOCTOR", "NURSE"
    private Long facilityId;
    private String email;
    private String password;
}
