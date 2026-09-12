package com.udhr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class InfermedicaService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${infermedica.app.id:}")
    private String appId;

    @Value("${infermedica.app.key:}")
    private String appKey;

    private static final String INFERMEDICA_API_URL = "https://api.infermedica.com/v3/triage";

    public Map<String, Object> getTriageRecommendation(String gender, int ageInYears, List<Map<String, String>> evidenceList) {
        // If credentials are not configured, use local fallback logic. This is
        // a deliberately conservative ruleset, not real medical triage -- log
        // it loudly so it's never mistaken for the real Infermedica engine.
        if (appId == null || appId.trim().isEmpty() || appKey == null || appKey.trim().isEmpty()) {
            System.err.println("WARNING: infermedica.app.id/app.key are not configured -- symptom " +
                    "triage is running on a limited local fallback, not the real Infermedica engine.");
            return getLocalFallbackTriage(evidenceList);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("App-Id", appId);
            headers.set("App-Key", appKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sex", gender.toLowerCase());
            
            Map<String, Object> ageMap = new HashMap<>();
            ageMap.put("value", ageInYears);
            ageMap.put("unit", "year");
            requestBody.put("age", ageMap);

            List<Map<String, Object>> requestEvidence = new ArrayList<>();
            for (Map<String, String> ev : evidenceList) {
                Map<String, Object> evMap = new HashMap<>();
                evMap.put("id", ev.get("id"));
                evMap.put("choice_id", ev.get("choice_id")); // "present", "absent", "unknown"
                requestEvidence.add(evMap);
            }
            requestBody.put("evidence", requestEvidence);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(INFERMEDICA_API_URL, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String triageLevel = (String) body.get("triage_level");
                String mappedUrgency = mapTriageLevelToUrgency(triageLevel);
                String recommendationText = getRecommendationText(mappedUrgency, triageLevel, (String) body.get("label"));

                Map<String, Object> result = new HashMap<>();
                result.put("urgencyLevel", mappedUrgency);
                result.put("recommendation", recommendationText);
                result.put("rawResponse", body);
                return result;
            }
        } catch (Exception e) {
            System.err.println("Failed to call Infermedica API, falling back to local logic. Error: " + e.getMessage());
        }

        return getLocalFallbackTriage(evidenceList);
    }

    // Unrecognized or missing triage levels default to YELLOW ("seek care"),
    // never GREEN ("you're fine") -- an API contract change or unexpected
    // value here should never be silently treated as an all-clear.
    private String mapTriageLevelToUrgency(String triageLevel) {
        if (triageLevel == null) return "YELLOW";
        switch (triageLevel.toLowerCase()) {
            case "emergency":
            case "emergency_ambulance":
                return "RED";
            case "self_care":
                return "GREEN";
            case "consultation_24":
            case "consultation":
            default:
                return "YELLOW";
        }
    }

    private String getRecommendationText(String urgency, String triageLevel, String label) {
        if ("RED".equals(urgency)) {
            return "🔴 Urgent attention required: Please go to the nearest emergency department immediately or call an ambulance. (API Triage: " + label + ")";
        } else if ("YELLOW".equals(urgency)) {
            return "🟡 Medical consultation recommended: Please visit a local clinic or primary care physician within the next 24 hours. (API Triage: " + label + ")";
        } else {
            return "🟢 Home care recommended: Your symptoms appear mild. Rest at home, stay hydrated, and monitor your condition. If symptoms worsen, please consult a clinic. (API Triage: " + label + ")";
        }
    }

    private Map<String, Object> getLocalFallbackTriage(List<Map<String, String>> evidenceList) {
        // Fallback logic using common symptom prefixes/IDs
        boolean hasRedFlag = false;
        boolean hasYellowFlag = false;

        for (Map<String, String> ev : evidenceList) {
            String id = ev.get("id");
            String choice = ev.get("choice_id");

            if ("present".equalsIgnoreCase(choice)) {
                // Severe Red Flag symptoms
                if ("s_50".equals(id) || "s_88".equals(id) || "s_kaggle_56".equals(id) || "s_kaggle_27".equals(id) || id.contains("chest_pain") || id.contains("shortness_of_breath") || id.contains("dyspnea")) {
                    hasRedFlag = true;
                }
                // Moderate Yellow Flag symptoms
                if ("s_98".equals(id) || "s_13".equals(id) || "s_370".equals(id) || "s_kaggle_25".equals(id) || "s_kaggle_41".equals(id) || id.contains("fever") || id.contains("abdominal") || id.contains("dizziness")) {
                    hasYellowFlag = true;
                }
            }
        }

        // This ruleset only recognizes a small, explicit list of symptom IDs.
        // A symptom that isn't on either list is UNRECOGNIZED, not "mild" --
        // defaulting that case to green would silently tell a patient with a
        // serious, unlisted symptom that it's safe to stay home. Default to
        // "seek care" instead.
        String urgency;
        String recommendation;
        if (hasRedFlag) {
            urgency = "RED";
            recommendation = "🔴 Urgent attention required: Based on your severe symptoms (such as chest pain or shortness of breath), please proceed to the nearest emergency department immediately. [Local Triage]";
        } else if (hasYellowFlag) {
            urgency = "YELLOW";
            recommendation = "🟡 Medical consultation recommended: Based on your symptoms (such as fever or abdominal pain), please visit a local health clinic within the next 24 hours. [Local Triage]";
        } else {
            urgency = "YELLOW";
            recommendation = "🟡 We can't confidently assess these symptoms with our limited local triage. Please visit a local health clinic or call a nurse hotline to be safe. [Local Triage - Unrecognized Symptoms]";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("urgencyLevel", urgency);
        result.put("recommendation", recommendation);
        result.put("rawResponse", Map.of("source", "local_fallback"));
        return result;
    }
}
