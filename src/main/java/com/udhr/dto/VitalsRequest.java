package com.udhr.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VitalsRequest {
    private Long patientId;
    private Long queueEntryId;
    private Long visitId;
    private Integer systolicBp;
    private Integer diastolicBp;
    private BigDecimal temperatureC;
    private Integer pulseBpm;
    private Integer respiratoryRate;
    private BigDecimal oxygenSaturation;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal glucoseMmol;
    private String notes;
}
