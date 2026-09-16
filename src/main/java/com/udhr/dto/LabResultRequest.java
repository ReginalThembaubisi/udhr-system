package com.udhr.dto;

import lombok.Data;

@Data
public class LabResultRequest {
    private Long patientId;
    private Long staffId; // optional: defaults to the logged-in staff member
    private Long facilityId; // optional: defaults to that staff member's facility
    private Long visitId; // optional: reuses the most recent visit, or creates one
    private String testName;
    private String result;
    private String unit;
    private String normalRange;
    private String notes;
}
