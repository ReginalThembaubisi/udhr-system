package com.udhr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Sends one plain-text email through Brevo's HTTP API (Render's free plan
 * blocks SMTP, so a normal mail server can't be used). The API key and the
 * sender address come from the BREVO_API_KEY and ALERT_EMAIL_FROM settings on
 * the host; with either missing, nothing is sent.
 */
@Component
public class AlertEmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(AlertEmailSender.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${udhr.alert-email.api-key:}")
    private String apiKey;

    @Value("${udhr.alert-email.from:}")
    private String from;

    private final RestTemplate http = new RestTemplate();

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank() && from != null && !from.isBlank();
    }

    /** True if Brevo accepted the email. */
    public boolean send(List<String> to, String subject, String text) {
        if (!configured() || to.isEmpty()) {
            return false;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey.trim());
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", "UDHR security", "email", from.trim()),
                "to", to.stream().map(address -> Map.of("email", address)).toList(),
                "subject", subject,
                "textContent", text);
        try {
            http.postForEntity(BREVO_URL, new HttpEntity<>(body, headers), String.class);
            return true;
        } catch (Exception e) {
            // Never log the key; the message from Brevo says what's wrong (bad key, unverified sender).
            LOG.warn("Security alert email not sent: {}", e.getMessage());
            return false;
        }
    }
}
