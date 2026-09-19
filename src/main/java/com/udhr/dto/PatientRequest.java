package com.udhr.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PatientRequest {
    private String idNumber;
    private String passportNumber;
    private String firstName;
    private String lastName;
    private String dateOfBirth; // Will be parsed to LocalDate
    private String gender;
    private String contactNumber;
    private String email;
    private String address;

    // Next of kin, all optional
    private String nextOfKinFirstName;
    private String nextOfKinLastName;
    private String nextOfKinRelationship;
    private String nextOfKinPhone;

    // Newborn / birth-record fields, all optional
    private String motherIdNumber; // links this file to the mother's existing record
    private Long birthFacilityId;
    private Integer birthWeightGrams;
    private BigDecimal birthLengthCm;
    private Integer apgarScore1Min;
    private Integer apgarScore5Min;
}
