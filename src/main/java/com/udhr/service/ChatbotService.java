package com.udhr.service;

import com.udhr.dto.ChatbotResponse;
import com.udhr.dto.HealthGuidanceResponse;
import com.udhr.model.DietaryGuideline;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Symptom;
import com.udhr.model.SymptomCheck;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.PrescriptionRepository;
import com.udhr.repository.SymptomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private SymptomRepository symptomRepository;

    @Autowired
    private SymptomCheckerService symptomCheckerService;

    @Autowired
    private HealthGuidanceService healthGuidanceService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    // Phrases that warrant an immediate emergency response, independent of the
    // seeded symptom list (which only covers Kaggle disease-symptom wording).
    private static final List<String> EMERGENCY_PHRASES = List.of(
            "can't breathe", "cant breathe", "difficulty breathing", "not breathing",
            "severe bleeding", "bleeding a lot", "heavy bleeding", "won't stop bleeding",
            "unconscious", "unresponsive", "collapsed", "passed out",
            "suicidal", "kill myself", "want to die", "overdose", "seizure",
            "heart attack", "stroke"
    );

    // Colloquial phrasing mapped to the exact seeded Symptom.name it corresponds to,
    // so free-text messages match the same symptom set the checkbox-based checker uses.
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("Vomiting", List.of("throwing up", "throw up", "puking", "vomit")),
            Map.entry("Breathlessness", List.of("short of breath", "shortness of breath", "trouble breathing", "breathless")),
            Map.entry("Stomach Pain", List.of("stomach ache", "stomach hurts", "my stomach")),
            Map.entry("Abdominal Pain", List.of("tummy ache", "tummy hurts", "belly ache")),
            Map.entry("Throat Irritation", List.of("sore throat", "throat hurts")),
            Map.entry("High Fever", List.of("high temperature", "burning up")),
            Map.entry("Mild Fever", List.of("fever", "temperature", "feverish")),
            Map.entry("Fatigue", List.of("tired", "exhausted", "no energy", "worn out")),
            Map.entry("Dizziness", List.of("dizzy", "light headed", "lightheaded")),
            Map.entry("Nausea", List.of("nauseous", "feel sick", "queasy")),
            Map.entry("Itching", List.of("itchy")),
            Map.entry("Runny Nose", List.of("sniffles")),
            Map.entry("Cold Hands And Feets", List.of("cold hands", "cold feet"))
    );

    public ChatbotResponse respond(String idNumber, String rawMessage) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) {
            return new ChatbotResponse(
                    "Tell me what you're feeling, or ask me about your medications, allergies, or diet.",
                    null, null);
        }
        String lower = message.toLowerCase();

        for (String phrase : EMERGENCY_PHRASES) {
            if (lower.contains(phrase)) {
                return new ChatbotResponse(
                        "This sounds like it could be a medical emergency. Please call emergency services or go to the nearest emergency department right away. If someone is with you, ask them to help.",
                        "RED", null);
            }
        }

        Set<Symptom> matched = new LinkedHashSet<>();
        for (Symptom symptom : symptomRepository.findAll()) {
            String name = symptom.getName().toLowerCase();
            boolean hit = lower.contains(name);
            if (!hit) {
                for (String synonym : SYNONYMS.getOrDefault(symptom.getName(), List.of())) {
                    if (lower.contains(synonym)) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) {
                matched.add(symptom);
            }
        }

        if (!matched.isEmpty()) {
            List<Long> symptomIds = matched.stream().map(Symptom::getId).collect(Collectors.toList());
            List<String> symptomNames = matched.stream().map(Symptom::getName).collect(Collectors.toList());
            SymptomCheck check = symptomCheckerService.performSymptomCheck(idNumber, symptomIds);
            String reply = "I picked up on: " + String.join(", ", symptomNames) + ".\n\n"
                    + check.getRecommendation().split("\\[")[0].trim();
            return new ChatbotResponse(reply, check.getUrgencyLevel(), symptomNames);
        }

        if (containsAny(lower, "hi", "hello", "hey")) {
            return new ChatbotResponse(
                    "Hi! I'm your health assistant. Describe any symptoms you're feeling (e.g. \"I have a headache and fever\") and I'll guide you on next steps, or ask me about your medications, allergies, or diet.",
                    null, null);
        }

        if (containsAny(lower, "diet", "eat", "food guideline", "avoid")) {
            return new ChatbotResponse(buildDietReply(idNumber), null, null);
        }

        if (containsAny(lower, "medication", "medicine", "prescription", "pills", "dose")) {
            return new ChatbotResponse(buildMedicationReply(patient), null, null);
        }

        if (lower.contains("allerg")) {
            return new ChatbotResponse(buildAllergyReply(idNumber), null, null);
        }

        return new ChatbotResponse(
                "I didn't catch a specific symptom or question there. Try describing how you feel, e.g. \"I have a cough and fever\", or ask about your medications, allergies, or diet.",
                null, null);
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String buildDietReply(String idNumber) {
        HealthGuidanceResponse guidance = healthGuidanceService.getPersonalGuidance(idNumber);
        List<DietaryGuideline> guidelines = guidance.getDietaryGuidelines();
        if (guidelines == null || guidelines.isEmpty()) {
            return "You don't have any specific dietary guidelines on file yet — this is usually based on chronic conditions your doctor has recorded. Ask your doctor for personalized dietary advice.";
        }
        StringBuilder sb = new StringBuilder("Based on your conditions (" + String.join(", ", guidance.getConditions()) + "), here's what's recommended:\n");
        for (DietaryGuideline g : guidelines) {
            sb.append(g.getFoodType().equals("EAT") ? "\n✓ Eat: " : "\n✕ Avoid: ").append(g.getFoodItem());
            if (g.getDescription() != null && !g.getDescription().isBlank()) {
                sb.append(" — ").append(g.getDescription());
            }
        }
        return sb.toString();
    }

    private String buildMedicationReply(Patient patient) {
        List<Prescription> active = prescriptionRepository.findByPatientIdAndActiveTrue(patient.getId());
        if (active.isEmpty()) {
            return "You don't have any active prescriptions on file right now.";
        }
        StringBuilder sb = new StringBuilder("Here are your active medications:\n");
        for (Prescription p : active) {
            sb.append("\n- ").append(p.getMedication()).append(" (").append(p.getDosage()).append("), ")
                    .append(p.getFrequency()).append(", until ").append(p.getEndDate());
        }
        return sb.toString();
    }

    private String buildAllergyReply(String idNumber) {
        HealthGuidanceResponse guidance = healthGuidanceService.getPersonalGuidance(idNumber);
        List<String> allergies = guidance.getAllergies();
        if (allergies == null || allergies.isEmpty()) {
            return "You don't have any allergies on file.";
        }
        return "Your recorded allergies: " + String.join(", ", allergies) + ". Always tell any new doctor or pharmacist about these before taking new medication.";
    }
}
