package com.udhr.dto;

import lombok.Data;

@Data
public class ReferralRequest {
    private Long patientId;
    private Long toFacilityId;
    private Long visitId; // optional: reuses the patient's active visit, or creates one
    private String urgency; // "ROUTINE" (default), "URGENT", "EMERGENCY"
    private String reason;
    private String clinicalSummary;
}
