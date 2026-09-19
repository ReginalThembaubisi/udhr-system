package com.udhr.dto;

import lombok.Data;

@Data
public class VitalsRequest {
    private String idNumber; // patient's national ID number — how the nurse looks the patient up
    private String bloodPressure;
    private Double temperatureCelsius;
    private Integer pulseBpm;
    private Integer respirationRate;
    private Integer oxygenSaturation;
    private Double weightKg;
    private Double heightCm;
    private String notes;
    private String reason; // reason for the visit, used only if a new visit needs to be opened
}
