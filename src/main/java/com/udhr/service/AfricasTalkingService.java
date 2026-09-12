package com.udhr.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private RestTemplate restTemplate;

    @Value("${africastalking.username:sandbox}")
    private String username;

    @Value("${africastalking.api.key:mock_key}")
    private String apiKey;

    public void sendSMS(String to, String message) {
        System.out.println("[Africa's Talking SMS Outbox] Destination: " + to + " | Message: " + message);

        if (apiKey == null || apiKey.trim().isEmpty() || "mock_key".equals(apiKey)) {
            System.out.println("[Africa's Talking SMS Mock Mode] API Key is set to default. SMS logged to console successfully.");
            return;
        }

        try {
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
                System.out.println("[Africa's Talking SMS Success] Response: " + response.getBody());
            } else {
                System.err.println("[Africa's Talking SMS Failure] Failed. Status code: " + response.getStatusCode() + " Response: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("[Africa's Talking SMS Exception] Error: " + e.getMessage());
        }
    }
}
