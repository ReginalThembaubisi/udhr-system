package com.udhr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class OpenFdaService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String OPEN_FDA_URL = "https://api.fda.gov/drug/label.json";

    public List<Map<String, Object>> getMedicationWarnings(String allergen) {
        if (allergen == null || allergen.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Construct the search query
            // Example: (warnings:penicillin+OR+contraindications:penicillin)+AND+allergy
            String searchQuery = String.format("(warnings:\"%s\" OR contraindications:\"%s\") AND (allergy OR hypersensitivity)", allergen, allergen);
            String url = OPEN_FDA_URL + "?search=" + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8) + "&limit=3";

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
            System.err.println("OpenFDA API request failed for allergen '" + allergen + "'. Error: " + e.getMessage());
        }

        // Never invent regulatory warning data. An empty result here is
        // rendered by the frontend as "no FDA alerts found -- consult your
        // doctor", which is honest whether that's because OpenFDA genuinely
        // has nothing on file or because the lookup itself failed.
        return Collections.emptyList();
    }
}
