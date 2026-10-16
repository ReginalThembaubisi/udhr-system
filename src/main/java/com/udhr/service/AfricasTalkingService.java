package com.udhr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class AfricasTalkingService {

    @Value("${africastalking.username:sandbox}")
    private String username;

    @Value("${africastalking.api.key:mock_key}")
    private String apiKey;

    public void sendSMS(String to, String message) {
        // Server logs are read by people who have no business seeing patient
        // details, so only a masked number is logged and never the message.
        String maskedTo = mask(to);

        if (apiKey == null || apiKey.trim().isEmpty() || "mock_key".equals(apiKey)) {
            System.out.println("[Africa's Talking SMS Mock Mode] No API key set; SMS to " + maskedTo + " not sent.");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            
            String url = "sandbox".equalsIgnoreCase(username) 
                    ? "https://api.sandbox.africastalking.com/version1/messaging"
                    : "https://api.africastalking.com/version1/messaging";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("apiKey", apiKey);
            headers.set("Accept", "application/json");

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("username", username);
            map.add("to", to);
            map.add("message", message);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("[Africa's Talking SMS] Sent to " + maskedTo);
            } else {
                System.err.println("[Africa's Talking SMS Failure] Sending to " + maskedTo + " failed with status " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("[Africa's Talking SMS Exception] Sending to " + maskedTo + " failed: " + e.getClass().getSimpleName());
        }
    }

    /** "0821234567" becomes "******4567". */
    static String mask(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
