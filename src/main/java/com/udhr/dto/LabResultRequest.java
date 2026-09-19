package com.udhr.dto;

import lombok.Data;

@Data
public class LabResultRequest {
    private String idNumber; // patient's national ID number — how staff look the patient up
    private String testName;
    private String result;
    private String unit;
    private String normalRange;
    private String notes;
}
