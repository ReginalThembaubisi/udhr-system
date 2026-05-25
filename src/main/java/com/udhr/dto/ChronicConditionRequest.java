package com.udhr.dto;

import lombok.Data;

@Data
public class ChronicConditionRequest {
    private Long patientId;
    private String conditionName;
    private String diagnosedDate; // LocalDate string
    private String notes;
}
