package com.udhr.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class OpenFdaService {

    private static final String OPEN_FDA_URL = "https://api.fda.gov/drug/label.json";

    public List<Map<String, Object>> getMedicationWarnings(String allergen) {
        if (allergen == null || allergen.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            // Construct the search query
            // Example: (warnings:penicillin+OR+contraindications:penicillin)+AND+allergy
            String searchQuery = String.format("(warnings:\"%s\" OR contraindications:\"%s\") AND (allergy OR hypersensitivity)", allergen, allergen);
            String url = OPEN_FDA_URL + "?search=" + searchQuery + "&limit=3";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");

                if (results != null && !results.isEmpty()) {
                    List<Map<String, Object>> warningList = new ArrayList<>();
                    for (Map<String, Object> result : results) {
                        Map<String, Object> warningDetails = new HashMap<>();

                        // Extract brand and generic names
                        Map<String, Object> openfda = (Map<String, Object>) result.get("openfda");
                        String brandName = "Unknown Medication";
                        String genericName = "Unknown Generic";

                        if (openfda != null) {
                            if (openfda.get("brand_name") != null) {
                                List<String> brands = (List<String>) openfda.get("brand_name");
                                brandName = String.join(", ", brands);
                            }
                            if (openfda.get("generic_name") != null) {
                                List<String> generics = (List<String>) openfda.get("generic_name");
                                genericName = String.join(", ", generics);
                            }
                        }

                        warningDetails.put("brandName", brandName);
                        warningDetails.put("genericName", genericName);

                        // Extract warnings / contraindications text
                        if (result.get("warnings") != null) {
                            List<String> warnings = (List<String>) result.get("warnings");
                            String warningText = String.join(" ", warnings);
                            if (warningText.length() > 300) {
                                warningText = warningText.substring(0, 300) + "...";
                            }
                            warningDetails.put("warningText", warningText);
                        } else if (result.get("contraindications") != null) {
                            List<String> contraindications = (List<String>) result.get("contraindications");
                            String contraText = String.join(" ", contraindications);
                            if (contraText.length() > 300) {
                                contraText = contraText.substring(0, 300) + "...";
                            }
                            warningDetails.put("warningText", contraText);
                        } else {
                            warningDetails.put("warningText", "Warning: Contains components that may trigger hypersensitivity reactions.");
                        }

                        warningList.add(warningDetails);
                    }
                    return warningList;
                }
            }
        } catch (Exception e) {
            System.err.println("OpenFDA API request failed for allergen '" + allergen + "'. Using local fallback. Error: " + e.getMessage());
        }

        // Return local fallbacks if API is unavailable or returns empty
        return getLocalFallbackWarnings(allergen);
    }

    private List<Map<String, Object>> getLocalFallbackWarnings(String allergen) {
        List<Map<String, Object>> warnings = new ArrayList<>();
        String normalized = allergen.toLowerCase();

        if (normalized.contains("penicillin")) {
            Map<String, Object> w1 = new HashMap<>();
            w1.put("brandName", "Amoxil, Augmentin, Pen-Vee K");
            w1.put("genericName", "Amoxicillin, Co-amoxiclav, Penicillin V");
            w1.put("warningText", "Contraindicated: Cross-reactivity is highly common. Avoid all beta-lactam antibiotics (penicillins, cephalosporins like Cephalexin). Use alternatives like Macrolides (Azithromycin, Erythromycin) if prescribed.");
            warnings.add(w1);
        } else if (normalized.contains("aspirin") || normalized.contains("nsaid")) {
            Map<String, Object> w1 = new HashMap<>();
            w1.put("brandName", "Ecotrin, Disprin, Nurofen, Voltaren");
            w1.put("genericName", "Aspirin, Ibuprofen, Diclofenac, Naproxen");
            w1.put("warningText", "Contraindicated: May cause severe bronchospasm or hives in aspirin-sensitive patients. Avoid all Non-Steroidal Anti-inflammatory Drugs (NSAIDs). Use Acetaminophen (Paracetamol) for mild pain relief.");
            warnings.add(w1);
        } else if (normalized.contains("sulfa") || normalized.contains("sulfonamide")) {
            Map<String, Object> w1 = new HashMap<>();
            w1.put("brandName", "Bactrim, Pazo");
            w1.put("genericName", "Sulfamethoxazole-Trimethoprim, Sulfasalazine");
            w1.put("warningText", "Contraindicated: High risk of severe cutaneous adverse reactions (Stevens-Johnson syndrome). Avoid sulfa antibiotics and sulfasalazine. Inform doctor before taking thiazide diuretics or sulfonylureas.");
            warnings.add(w1);
        } else {
            Map<String, Object> w1 = new HashMap<>();
            w1.put("brandName", "Medications containing " + allergen);
            w1.put("genericName", allergen + " related compounds");
            w1.put("warningText", "Warning: Patient has a documented allergy to " + allergen + ". Avoid taking any medication containing this substance or its derivatives. Monitor closely for signs of hives, swelling, or breathing difficulty.");
            warnings.add(w1);
        }

        return warnings;
    }
}
