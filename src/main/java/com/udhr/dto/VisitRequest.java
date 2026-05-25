package com.udhr.dto;

import lombok.Data;

@Data
public class VisitRequest {
    private Long patientId;
    private Long staffId;
    private Long facilityId;
    private String reason;
    private String notes;
}
