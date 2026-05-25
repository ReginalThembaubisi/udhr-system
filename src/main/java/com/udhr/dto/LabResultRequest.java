package com.udhr.dto;

import lombok.Data;

@Data
public class LabResultRequest {
    private Long patientId;
    private Long staffId;
    private Long facilityId;
    private Long visitId;
    private String testName;
    private String result;
    private String unit;
    private String normalRange;
    private String notes;
}
