package com.udhr.dto;

import lombok.Data;

@Data
public class DispenseRequest {
    private Long prescriptionId;
    private String quantityDispensed;
    private Integer daysSupply;
    private String pharmacyNotes;
}
