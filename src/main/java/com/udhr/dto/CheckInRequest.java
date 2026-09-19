package com.udhr.dto;

import lombok.Data;

@Data
public class CheckInRequest {
    private String idNumber; // patient's national ID number
    private String reason;   // optional — why they're here today
}
