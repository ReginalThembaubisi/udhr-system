package com.udhr.dto;

import lombok.Data;

@Data
public class ImmunizationRequest {
    private Long patientId;
    private String vaccineName;
    private Integer doseNumber;
    private String scheduledDate; // optional, defaults to today; parsed to LocalDate
    private String administeredDate; // optional; if set, record is created as already GIVEN
    private String notes;
}
