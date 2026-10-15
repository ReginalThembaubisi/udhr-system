package com.udhr.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MedicationWarningServiceTest {

    private final MedicationWarningService service = new MedicationWarningService();

    @Test
    void penicillinAllergyNamesThePenicillins() {
        String list = service.getMedicationWarnings("Penicillin").toString();
        assertThat(list).contains("Amoxicillin").contains("Augmentin").doesNotContain("Unknown");
    }

    @Test
    void sulfaAllergyNamesSulfaDrugsOnly() {
        String list = service.getMedicationWarnings("sulfa").toString();
        assertThat(list).contains("Co-trimoxazole").doesNotContainIgnoringCase("lidocaine").doesNotContainIgnoringCase("cidofovir");
    }

    @Test
    void unknownAllergenGetsGeneralAdviceNotAnInventedDrug() {
        String list = service.getMedicationWarnings("Latex").toString();
        assertThat(list).contains("Any medicine containing Latex").doesNotContain("Unknown");
    }
}
