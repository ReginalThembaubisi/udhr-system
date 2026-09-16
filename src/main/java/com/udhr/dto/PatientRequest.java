package com.udhr.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PatientRequest {
    private String idNumber; // optional: may not exist yet (newborn, undocumented patient)
    private String passportNumber; // optional: alternative identifier for foreign nationals
    private String firstName;
    private String lastName;
    private String dateOfBirth; // Will be parsed to LocalDate
    private String gender;
    private String contactNumber;
    private String address;

    // Newborn / birth-record fields, all optional
    private String motherIdNumber; // links this file to the mother's existing record
    private Long birthFacilityId;
    private Integer birthWeightGrams;
    private BigDecimal birthLengthCm;
    private Integer apgarScore1Min;
    private Integer apgarScore5Min;
}
