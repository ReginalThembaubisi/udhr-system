package com.udhr.dto;

import lombok.Data;

@Data
public class PortalPinRequest {
    private String identifier; // ID number, MRN or passport number
    private String pin;        // 6 digits
}
