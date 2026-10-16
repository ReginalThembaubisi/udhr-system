package com.udhr.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    // RFC 6238 appendix B uses the ASCII secret "12345678901234567890" (SHA-1);
    // its 8-digit codes end in these 6 digits.
    private static final String RFC_SECRET = TotpService.base32Encode("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test
    void matchesTheRfc6238TestVectors() {
        assertThat(TotpService.codeAt(RFC_SECRET, 59 / 30)).isEqualTo("287082");
        assertThat(TotpService.codeAt(RFC_SECRET, 1111111109L / 30)).isEqualTo("081804");
        assertThat(TotpService.codeAt(RFC_SECRET, 1234567890L / 30)).isEqualTo("005924");
    }

    @Test
    void base32RoundTrips() {
        byte[] bytes = {1, 2, 3, (byte) 250, 99};
        assertThat(TotpService.base32Decode(TotpService.base32Encode(bytes))).containsExactly(bytes);
    }
}
