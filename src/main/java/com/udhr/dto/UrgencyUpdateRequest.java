package com.udhr.dto;

import lombok.Data;

@Data
public class UrgencyUpdateRequest {
    private String urgency; // "GREEN", "YELLOW", "RED"
}
