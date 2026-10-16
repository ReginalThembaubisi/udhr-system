package com.udhr.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MedicationCatalogTest {

    private final MedicationCatalog catalog = new MedicationCatalog();

    @Test
    void brandsAndGenericsAreTheSameMedicine() {
        assertThat(catalog.identify("Panado 500mg").orElseThrow().generic()).isEqualTo("paracetamol");
        assertThat(catalog.identify("Augmentin 625mg").orElseThrow().generic()).isEqualTo("co-amoxiclav");
        assertThat(catalog.identify("Allergex 4mg").orElseThrow().drugClass()).isEqualTo("antihistamine");
    }

    @Test
    void shortNamesOnlyMatchWholeWords() {
        assertThat(catalog.identify("Ventolin inhaler").orElseThrow().generic()).isEqualTo("salbutamol");
        assertThat(catalog.identify("Fexofenadine 180mg").orElseThrow().generic()).isEqualTo("fexofenadine");
        assertThat(catalog.identify("Unknown Inhaler")).isEmpty();
    }

    @Test
    void differentAntiretroviralsAreNeverTheSameMedicine() {
        assertThat(catalog.identify("TLD").orElseThrow().generic())
                .isNotEqualTo(catalog.identify("Efavirenz 600mg").orElseThrow().generic());
        assertThat(catalog.identify("TLD").orElseThrow().noSubstitute()).isTrue();
    }
}
