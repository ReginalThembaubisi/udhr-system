package com.udhr.dto;

import lombok.Data;

@Data
public class ImmunizationAdministerRequest {
    private String administeredDate; // optional, defaults to today; parsed to LocalDate
    private String notes;
}
