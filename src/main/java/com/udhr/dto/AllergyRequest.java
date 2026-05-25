package com.udhr.dto;

import lombok.Data;

@Data
public class AllergyRequest {
    private Long patientId;
    private String allergen;
    private String severity; // "MILD", "MODERATE", "SEVERE"
    private String notes;
}
