package com.udhr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientCheckResult {
    private String name;
    private String status; // "SAFE", "CAUTION", "DANGER"
    private String reason;
}
