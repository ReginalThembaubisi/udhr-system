package com.udhr.dto;

import lombok.Data;

@Data
public class FacilityRequest {
    private String name;
    private String type; // "CLINIC" or "HOSPITAL"
    private String province;
    private String address;
}
