package com.udhr.dto;

import com.udhr.model.DietaryGuideline;
import com.udhr.model.HealthTip;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthGuidanceResponse {
    private List<String> conditions;
    private List<String> allergies;
    private List<DietaryGuideline> dietaryGuidelines;
    private List<HealthTip> healthTips;
    private Map<String, List<Map<String, Object>>> medicationWarnings; // Allergen -> List of drug warnings
}
