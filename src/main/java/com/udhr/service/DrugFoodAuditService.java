package com.udhr.service;

import com.udhr.model.FoodScan;
import com.udhr.model.Prescription;
import com.udhr.repository.FoodScanRepository;
import com.udhr.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DrugFoodAuditService {

    @Autowired
    private FoodScanRepository foodScanRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    public List<Map<String, Object>> auditDrugFoodConflicts(Long patientId) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        List<Prescription> activePrescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        List<FoodScan> scans = foodScanRepository.findByPatientIdOrderByCreatedAtDesc(patientId);

        if (activePrescriptions.isEmpty() || scans.isEmpty()) {
            return conflicts;
        }

        // Tokenize and normalize all ingredients checked in the past scans
        Set<String> checkedIngredients = new HashSet<>();
        for (FoodScan scan : scans) {
            String[] tokens = scan.getIngredients().toLowerCase().split("[,;\\.\\(\\)\\s]+");
            for (String t : tokens) {
                String clean = t.replaceAll("[^a-zA-Z]", "");
                if (clean.length() > 2) {
                    checkedIngredients.add(clean);
                }
            }
        }

        for (Prescription rx : activePrescriptions) {
            if (!rx.getActive()) continue;

            String medLower = rx.getMedication().toLowerCase();

            // 1. Metformin/Diabetes Conflicts
            if (medLower.contains("metformin") || medLower.contains("gliclazide") || medLower.contains("insulin")) {
                String[] diabeticTriggers = {"pap", "mageu", "bread", "sugar", "fructose", "glucose", "sucrose", "dextrose", "syrup", "honey", "maltodextrin"};
                for (String trigger : diabeticTriggers) {
                    if (checkedIngredients.contains(trigger)) {
                        Map<String, Object> conflict = new HashMap<>();
                        conflict.put("medication", rx.getMedication());
                        conflict.put("ingredient", trigger);
                        conflict.put("severity", "MEDIUM");
                        conflict.put("message", String.format("⚠️ Warning: Consuming '%s' is not recommended while taking '%s'. High Glycemic Index carbohydrates and sugars raise blood glucose rapidly, directly counteracting the medication's therapeutic effect.", trigger, rx.getMedication()));
                        conflicts.add(conflict);
                    }
                }
            }

            // 2. Amlodipine/Hypertension Conflicts
            if (medLower.contains("amlodipine") || medLower.contains("enalapril") || medLower.contains("losartan") || medLower.contains("hydrochlorothiazide")) {
                String[] hypertensiveTriggers = {"salt", "sodium", "msg", "glutamate", "bicarbonate", "chips", "boerewors", "polony", "biltong", "trans fat", "hydrogenated"};
                for (String trigger : hypertensiveTriggers) {
                    if (checkedIngredients.contains(trigger)) {
                        Map<String, Object> conflict = new HashMap<>();
                        conflict.put("medication", rx.getMedication());
                        conflict.put("ingredient", trigger);
                        conflict.put("severity", "MEDIUM");
                        conflict.put("message", String.format("⚠️ Warning: High sodium/saturated fat component '%s' increases fluid retention and blood volume, directly opposing the vasodilatory action of '%s'.", trigger, rx.getMedication()));
                        conflicts.add(conflict);
                    }
                }
            }

            // 3. Warfarin Conflicts (Vitamin K)
            if (medLower.contains("warfarin")) {
                String[] warfarinTriggers = {"spinach", "broccoli", "kale", "cabbage", "morogo", "greens"};
                for (String trigger : warfarinTriggers) {
                    if (checkedIngredients.contains(trigger)) {
                        Map<String, Object> conflict = new HashMap<>();
                        conflict.put("medication", rx.getMedication());
                        conflict.put("ingredient", trigger);
                        conflict.put("severity", "CRITICAL");
                        conflict.put("message", String.format("❌ Danger: High Vitamin K content in '%s' promotes synthesis of clotting factors in the liver, directly neutralizing the anticoagulant action of Warfarin and increasing thromboembolism risk.", trigger));
                        conflicts.add(conflict);
                    }
                }
            }
        }

        return conflicts;
    }
}
