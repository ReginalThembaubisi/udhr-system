package com.udhr.dto;

import lombok.Data;

@Data
public class QueueCheckInRequest {
    private Long patientId;
    private String department; // "GP", "DENTAL", "MATERNITY", "PEDIATRICS", "CASUALTY", "CHRONIC_CLUB"
    private String reason;
    private String urgency; // optional: "GREEN" (default), "YELLOW", "RED"
}
