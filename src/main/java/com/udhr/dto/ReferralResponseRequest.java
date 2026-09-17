package com.udhr.dto;

import lombok.Data;

@Data
public class ReferralResponseRequest {
    private String status; // "ACCEPTED", "DECLINED", "COMPLETED", "CANCELLED"
    private String responseNotes;
}
