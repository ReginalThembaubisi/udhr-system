package com.udhr.service;

import java.security.SecureRandom;
import java.util.Locale;

/** Generates a cryptographically random six-digit temporary patient portal PIN. */
public final class PortalPinGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PortalPinGenerator() { }

    public static String generate() {
        // Formatting keeps leading zeros (for example, 004173).
        String pin;
        do {
            pin = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        } while (pin.chars().distinct().count() == 1 || "123456".equals(pin) || "654321".equals(pin));
        return pin;
    }
}
