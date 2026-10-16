package com.udhr.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** SMS content and logs carry no health details (POPIA section 32). */
class NotificationPrivacyTest {

    @Test
    void reminderTextNamesNoMedicine() {
        assertThat(NotificationScheduler.reminderText("Thandi", "08:00"))
                .contains("Thandi").contains("08:00").doesNotContainIgnoringCase("tenofovir");
    }

    @Test
    void phoneNumbersAreMaskedInLogs() {
        assertThat(AfricasTalkingService.mask("0821234567")).isEqualTo("******4567");
        assertThat(AfricasTalkingService.mask(null)).isEqualTo("****");
    }
}
