package com.udhr.dto;

import lombok.Data;

@Data
public class DischargeRequest {
    private Long patientId;
    private Long visitId; // optional: defaults to the patient's active visit
    private String dischargeOutcome; // "HOME", "DECEASED", "ABSCONDED", "TRANSFERRED"
    private String dischargeSummary;
    private String followUpDate; // optional, YYYY-MM-DD
}
