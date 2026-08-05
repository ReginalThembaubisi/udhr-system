package com.udhr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
public class SymptomCheckerService {

    @Autowired
    private SymptomRepository symptomRepository;

    @Autowired
    private SymptomCheckRepository symptomCheckRepository;

    @Autowired
    private SymptomCheckDetailRepository symptomCheckDetailRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private InfermedicaService infermedicaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Symptom> getAllSymptoms() {
        return symptomRepository.findAll();
    }

    @Transactional
    public SymptomCheck performSymptomCheck(String patientIdNumber, List<Long> symptomIds) {
        Patient patient = patientRepository.findByIdNumber(patientIdNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        List<Symptom> selectedSymptoms = symptomRepository.findAllById(symptomIds);
        if (selectedSymptoms.isEmpty()) {
            throw new RuntimeException("No valid symptoms selected");
        }

        // Calculate age
        int age = Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
        String gender = patient.getGender();
        if (gender == null || (!gender.equalsIgnoreCase("male") && !gender.equalsIgnoreCase("female"))) {
            gender = "female"; // Default fallback for Infermedica sex field (accepts male/female)
        }

        // Build evidence list
        List<Map<String, String>> evidenceList = new ArrayList<>();
        for (Symptom symptom : selectedSymptoms) {
            Map<String, String> ev = new HashMap<>();
            ev.put("id", symptom.getInfermedicaId());
            ev.put("choice_id", "present");
            evidenceList.add(ev);
        }

        // Get recommendation from Infermedica
        Map<String, Object> triageResult = infermedicaService.getTriageRecommendation(gender, age, evidenceList);
        String urgencyLevel = (String) triageResult.get("urgencyLevel");
        String recommendation = (String) triageResult.get("recommendation");

        String rawResponseJson = "";
        try {
            rawResponseJson = objectMapper.writeValueAsString(triageResult.get("rawResponse"));
        } catch (Exception e) {
            rawResponseJson = "{}";
        }

        // Save SymptomCheck
        SymptomCheck symptomCheck = new SymptomCheck();
        symptomCheck.setPatient(patient);
        symptomCheck.setUrgencyLevel(urgencyLevel);
        symptomCheck.setRecommendation(recommendation);
        symptomCheck.setApiResponseDump(rawResponseJson);
        symptomCheck = symptomCheckRepository.save(symptomCheck);

        // Save SymptomCheckDetails
        for (Symptom symptom : selectedSymptoms) {
            SymptomCheckDetail detail = new SymptomCheckDetail();
            detail.setSymptomCheck(symptomCheck);
            detail.setSymptom(symptom);
            detail.setChoiceId("present");
            symptomCheckDetailRepository.save(detail);
        }

        return symptomCheck;
    }

    public List<SymptomCheck> getPatientHistory(String patientIdNumber) {
        Patient patient = patientRepository.findByIdNumber(patientIdNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        return symptomCheckRepository.findByPatientIdOrderByCheckedAtDesc(patient.getId());
    }
}
