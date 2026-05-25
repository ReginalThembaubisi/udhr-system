package com.udhr.dto;

import lombok.Data;

@Data
public class DiagnosisRequest {
    private Long patientId;
    private Long visitId;
    private Long doctorId;
    private Long facilityId;
    private String diagnosis;
    private String notes;
}
