package com.udhr.service;

import com.udhr.dto.HealthGuidanceResponse;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HealthGuidanceService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private ChronicConditionRepository chronicConditionRepository;

    @Autowired
    private HealthTipRepository healthTipRepository;

    @Autowired
    private DietaryGuidelineRepository dietaryGuidelineRepository;

    @Autowired
    private OpenFdaService openFdaService;

    public HealthGuidanceResponse getPersonalGuidance(String patientIdNumber) {
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

        // Retrieve health tips and dietary guidelines for matching conditions
        List<HealthTip> healthTips = new ArrayList<>();
        List<DietaryGuideline> dietaryGuidelines = new ArrayList<>();

        if (!conditions.isEmpty()) {
            healthTips = healthTipRepository.findByConditionNameIn(conditions);
            dietaryGuidelines = dietaryGuidelineRepository.findByConditionNameIn(conditions);
        }

        // Retrieve FDA medication warnings for each allergy
        Map<String, List<Map<String, Object>>> medicationWarnings = new HashMap<>();
        for (String allergen : allergies) {
            List<Map<String, Object>> warnings = openFdaService.getMedicationWarnings(allergen);
            medicationWarnings.put(allergen, warnings);
        }

        return new HealthGuidanceResponse(
                conditions,
                allergies,
                dietaryGuidelines,
                healthTips,
                medicationWarnings
        );
    }
}
