package com.udhr.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Refuses to start the application on a weak or missing JWT secret.
 *
 * The secret is the only thing standing between a stranger and a valid token
 * for any role in the system. If it is guessable — or, worse, committed to a
 * public repository — anyone can mint a token that says they are a doctor and
 * read every patient record, with no password and nothing to steal first.
 *
 * Failing to start is deliberate. A warning in the log would be ignored, and a
 * fallback default would quietly leave the system wide open while looking
 * perfectly healthy.
 */
@Component
public class JwtSecretValidator {

    // HS256 signing keys must be at least 256 bits. Shorter keys are rejected by
    // the JWT library at runtime anyway — better to say so at startup than to
    // discover it when the first person tries to sign in.
    private static final int MINIMUM_BYTES = 32;

    // Secrets that have appeared in this repository's history, or are obvious
    // placeholders. Anyone reading the public repo knows them.
    private static final String[] KNOWN_WEAK_SECRETS = {
            "udhr-secret-key-2026-university-mpumalanga",
            "secret",
            "changeme",
            "your-secret-key"
    };

    @Value("${jwt.secret:}")
    private String secret;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(buildMessage(
                    "No JWT secret is set."));
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_BYTES) {
            throw new IllegalStateException(buildMessage(
                    "The JWT secret is too short — it must be at least " + MINIMUM_BYTES + " characters."));
        }

        for (String weak : KNOWN_WEAK_SECRETS) {
            if (secret.equalsIgnoreCase(weak)) {
                throw new IllegalStateException(buildMessage(
                        "The JWT secret is a known placeholder value that appears in this repository."));
            }
        }
    }

    private String buildMessage(String problem) {
        return String.join(System.lineSeparator(),
                "",
                "============================================================",
                "  UDHR cannot start: " + problem,
                "",
                "  The JWT secret signs every login token. A guessable or",
                "  published secret lets anyone create a token for any role",
                "  and read every patient record.",
                "",
                "  Set a strong, private value in the JWT_SECRET environment",
                "  variable. Generate one with:",
                "",
                "      openssl rand -base64 48",
                "",
                "  For local development, start.sh / start.ps1 set a throwaway",
                "  value for you.",
                "============================================================",
                "");
    }
}
