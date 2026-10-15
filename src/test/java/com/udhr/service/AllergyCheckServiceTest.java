package com.udhr.service;

import com.udhr.model.Allergy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AllergyCheckServiceTest {

    private final AllergyCheckService checker = new AllergyCheckService();

    private static Allergy allergy(String allergen, String severity) {
        Allergy a = new Allergy();
        a.setAllergen(allergen);
        a.setSeverity(severity);
        return a;
    }

    @Test
    void penicillinAllergyCatchesPenicillinsByOtherNames() {
        List<Allergy> allergies = List.of(allergy("Penicillin", "SEVERE"));
        assertThat(checker.findConflicts("Amoxicillin 500mg", allergies)).hasSize(1);
        assertThat(checker.findConflicts("Augmentin 625mg", allergies)).hasSize(1);
        assertThat(checker.findConflicts("Flucloxacillin", allergies)).hasSize(1);
        assertThat(checker.findConflicts("Amoxicillin 500mg", allergies).get(0))
                .contains("penicillin group").contains("SEVERE").contains("Penicillin");
    }

    @Test
    void penicillinAllergyFlagsCephalosporinCrossReaction() {
        List<String> warnings = checker.findConflicts("Cephalexin 500mg", List.of(allergy("Penicillin", "MODERATE")));
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0)).contains("possible cross-reaction");
    }

    @Test
    void otherDrugGroups() {
        assertThat(checker.findConflicts("Co-trimoxazole 960mg", List.of(allergy("Sulfa", "SEVERE")))).hasSize(1);
        assertThat(checker.findConflicts("Ibuprofen 400mg", List.of(allergy("Aspirin", "MODERATE")))).hasSize(1);
        assertThat(checker.findConflicts("Tramadol 50mg", List.of(allergy("Codeine", "MILD")))).hasSize(1);
    }

    @Test
    void directNameMatchIsCaseInsensitive() {
        assertThat(checker.findConflicts("ASPIRIN 300MG", List.of(allergy("aspirin", "SEVERE")))).hasSize(1);
    }

    @Test
    void unrelatedMedicinesPassClean() {
        List<Allergy> allergies = List.of(allergy("Penicillin", "SEVERE"), allergy("Peanuts", "SEVERE"));
        assertThat(checker.findConflicts("Paracetamol 500mg", allergies)).isEmpty();
        assertThat(checker.findConflicts("Metformin 500mg", allergies)).isEmpty();
        assertThat(checker.findConflicts("Azithromycin 500mg", allergies)).isEmpty();
    }

    @Test
    void noAllergiesNoWarnings() {
        assertThat(checker.findConflicts("Amoxicillin", List.of())).isEmpty();
        assertThat(checker.findConflicts(null, List.of(allergy("Penicillin", "SEVERE")))).isEmpty();
    }
}
