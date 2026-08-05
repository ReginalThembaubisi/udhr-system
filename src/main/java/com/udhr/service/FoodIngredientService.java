package com.udhr.service;

import com.udhr.dto.IngredientCheckResult;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FoodIngredientService {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private IngredientWarningRepository ingredientWarningRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ChronicConditionRepository chronicConditionRepository;

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private FoodScanRepository foodScanRepository;

    public List<IngredientCheckResult> checkIngredients(String patientIdNumber, String rawIngredients) {
        Patient patient = patientRepository.findByIdNumber(patientIdNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Fetch patient conditions and allergies
        List<ChronicCondition> conditionsList = chronicConditionRepository.findByPatientId(patient.getId());
        List<Allergy> allergiesList = allergyRepository.findByPatientId(patient.getId());

        List<String> conditions = conditionsList.stream()
                .map(ChronicCondition::getConditionName)
                .collect(Collectors.toList());

        List<String> allergies = allergiesList.stream()
                .map(Allergy::getAllergen)
                .collect(Collectors.toList());

        // Parse and tokenize raw ingredients
        List<String> parsedIngredients = parseIngredientsText(rawIngredients);
        List<IngredientCheckResult> results = new ArrayList<>();

        for (String ingredientName : parsedIngredients) {
            results.add(analyzeIngredient(ingredientName, conditions, allergies));
        }

        try {
            FoodScan scan = new FoodScan();
            scan.setPatient(patient);
            scan.setIngredients(rawIngredients);
            foodScanRepository.save(scan);
        } catch (Exception e) {
            System.err.println("Failed to persist food scan history: " + e.getMessage());
        }

        return results;
    }

    private List<String> parseIngredientsText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Clean initial prefixes (e.g. "Ingredients:", "Contains less than 2% of")
        String cleaned = text.replaceAll("(?i)^ingredients:\\s*", "")
                            .replaceAll("(?i)^contains:\\s*", "")
                            .replaceAll("(?i)\\bcontains less than 2% of:?\\s*", "");

        // Split by standard separators: commas, semicolons, periods, or parentheses
        String[] tokens = cleaned.split("[,;\\.\\(\\)]");
        List<String> list = new ArrayList<>();

        for (String t : tokens) {
            String trimmed = t.trim();
            // Remove numbers, percentages (e.g. "2%", "less than")
            String cleanToken = trimmed.replaceAll("\\d+%", "")
                                       .replaceAll("(?i)\\b(less than|enriched|bleached|organic|natural|artificial)\\b", "")
                                       .replaceAll("\\s+", " ")
                                       .trim();

            if (!cleanToken.isEmpty() && cleanToken.length() > 2) {
                // Avoid duplicates
                if (!list.contains(cleanToken)) {
                    list.add(cleanToken);
                }
            }
        }

        return list;
    }

    private IngredientCheckResult analyzeIngredient(String name, List<String> conditions, List<String> allergies) {
        String normalized = name.toLowerCase();

        // 1. Check Allergies (Direct or contains allergen name)
        for (String allergy : allergies) {
            String normAllergy = allergy.toLowerCase();
            if (normalized.contains(normAllergy) || normAllergy.contains(normalized)) {
                return new IngredientCheckResult(name, "DANGER", "❌ Danger: Contains allergen '" + allergy + "' (Matches patient's allergy file).");
            }
            // Broad category checks (e.g. "Nuts" matching "Peanuts")
            if (normAllergy.contains("nut") && (normalized.contains("peanut") || normalized.contains("almond") || normalized.contains("walnut") || normalized.contains("cashew"))) {
                return new IngredientCheckResult(name, "DANGER", "❌ Danger: Matches patient's allergy file for '" + allergy + "' (Nut group cross-reactivity).");
            }
        }

        // 2. Check database mappings
        Optional<Ingredient> ingredientOpt = ingredientRepository.findByNameIgnoreCase(name);
        if (ingredientOpt.isPresent()) {
            Ingredient ingredient = ingredientOpt.get();
            List<IngredientWarning> warnings = ingredientWarningRepository.findByIngredientIdAndConditionNameIn(ingredient.getId(), conditions);

            if (!warnings.isEmpty()) {
                // Sort by severity (DANGER > CAUTION > SAFE)
                IngredientWarning worstWarning = warnings.stream()
                        .min((w1, w2) -> getSeverityWeight(w2.getSeverity()) - getSeverityWeight(w1.getSeverity()))
                        .get();

                String status = worstWarning.getSeverity();
                String icon = "SAFE".equals(status) ? "✅ " : "CAUTION".equals(status) ? "⚠️ " : "❌ ";
                return new IngredientCheckResult(name, status, icon + status + ": " + worstWarning.getReason());
            }
        }

        // 3. Clinical Fail-Safe Rules (if not mapped in DB or for synonyms)
        // Diabetes sweetener checks
        if (conditions.contains("Diabetes")) {
            if (normalized.contains("sugar") || normalized.contains("fructose") || normalized.contains("glucose") || 
                normalized.contains("syrup") || normalized.contains("dextrose") || normalized.contains("sucrose") || 
                normalized.contains("maltodextrin") || normalized.contains("honey")) {
                return new IngredientCheckResult(name, "DANGER", "❌ Danger: Fast-acting sweetener. High Glycemic Index food items cause sudden, dangerous spikes in blood glucose levels.");
            }
        }

        // Hypertension sodium checks
        if (conditions.contains("Hypertension")) {
            if (normalized.contains("sodium") || normalized.contains("salt") || normalized.contains("msg") || 
                normalized.contains("monosodium glutamate") || normalized.contains("bicarbonate")) {
                return new IngredientCheckResult(name, "DANGER", "❌ Danger: High sodium component. Retains body fluids, increases blood volume, and raises blood pressure.");
            }
            if (normalized.contains("trans fat") || normalized.contains("hydrogenated")) {
                return new IngredientCheckResult(name, "CAUTION", "⚠️ Caution: Trans fats raise bad cholesterol (LDL) and lower good cholesterol (HDL), worsening hypertension complications.");
            }
        }

        // 4. Default Safe response
        return new IngredientCheckResult(name, "SAFE", "✅ Safe: Fine for your medical conditions.");
    }

    private int getSeverityWeight(String severity) {
        if ("DANGER".equalsIgnoreCase(severity)) return 3;
        if ("CAUTION".equalsIgnoreCase(severity)) return 2;
        return 1;
    }
}
