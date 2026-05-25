package com.udhr.dto;

import lombok.Data;

@Data
public class PrescriptionRequest {
    private Long patientId;
    private Long doctorId;
    private Long facilityId;
    private Long visitId;
    private String medication;
    private String dosage;
    private String frequency;
    private String startDate; // LocalDate string
    private String endDate;   // LocalDate string
    private String notes;
}
