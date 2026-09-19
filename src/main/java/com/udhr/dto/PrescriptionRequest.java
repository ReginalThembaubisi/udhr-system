package com.udhr.dto;

import lombok.Data;

@Data
public class PrescriptionRequest {
    private Long patientId;
    private Long doctorId;
    private Long facilityId;
    private Long visitId;
    private String medication;
    private String medicationName; // Added for frontend mapping compatibility
    private String dosage;
    private String frequency;
    private String startDate; // LocalDate string
    private String endDate;   // LocalDate string
    private Integer durationDays; // Added for frontend mapping compatibility
    private String notes;
    private String dispenseMethod; // "PHARMACY" (default) or "SELF" — doctor's choice at the point of prescribing
}
