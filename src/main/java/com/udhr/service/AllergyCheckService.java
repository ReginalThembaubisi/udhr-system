package com.udhr.service;

import com.udhr.model.Allergy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Checks a medication against a patient's recorded allergies before it is
 * prescribed or dispensed.
 *
 * Matching on the allergen's own name is not enough: a patient recorded as
 * "Penicillin" allergic must also be stopped from getting amoxicillin or
 * Augmentin, which never contain the word "penicillin". So each allergen is
 * mapped to the drug groups it belongs to, and the medication is checked
 * against every drug in those groups.
 *
 * This is a safety net for the common, well-known cross-reactions, not a full
 * drug database: a medication it doesn't recognise produces no warning, so the
 * prescriber's own check still matters.
 */
@Service
public class AllergyCheckService {

    private record DrugGroup(String name, List<String> allergenWords, List<String> drugs, String note) {}

    private static final List<DrugGroup> GROUPS = List.of(
            new DrugGroup("penicillin",
                    List.of("penicillin", "amoxicillin", "amoxycillin", "ampicillin", "augmentin",
                            "co-amoxiclav", "flucloxacillin", "cloxacillin", "beta-lactam", "beta lactam"),
                    List.of("penicillin", "amoxicillin", "amoxycillin", "amoxil", "moxypen", "ampicillin",
                            "augmentin", "co-amoxiclav", "coamoxiclav", "clavulan", "flucloxacillin",
                            "cloxacillin", "benzathine", "piperacillin", "tazocin", "phenoxymethylpenicillin"),
                    null),
            // Not the same group, but a known cross-reaction for penicillin-allergic
            // patients — flagged so the prescriber decides, rather than silently allowed.
            new DrugGroup("cephalosporin",
                    List.of("penicillin", "amoxicillin", "ampicillin", "cephalosporin", "cephalexin",
                            "cefalexin", "ceftriaxone", "beta-lactam", "beta lactam"),
                    List.of("cephalexin", "cefalexin", "cefuroxime", "ceftriaxone", "cefazolin",
                            "cefixime", "cefpodoxime", "cefadroxil", "cefotaxime", "ceftazidime"),
                    "possible cross-reaction"),
            new DrugGroup("sulfonamide",
                    List.of("sulfa", "sulpha", "sulfonamide", "sulphonamide", "co-trimoxazole",
                            "cotrimoxazole", "bactrim", "sulfamethoxazole", "sulphamethoxazole"),
                    List.of("co-trimoxazole", "cotrimoxazole", "bactrim", "septran", "sulfamethoxazole",
                            "sulphamethoxazole", "sulfadiazine", "sulfasalazine"),
                    null),
            new DrugGroup("NSAID / aspirin",
                    List.of("nsaid", "aspirin", "ibuprofen", "diclofenac", "naproxen", "anti-inflammator"),
                    List.of("aspirin", "disprin", "ibuprofen", "nurofen", "brufen", "diclofenac", "voltaren",
                            "naproxen", "mefenamic", "ponstan", "indomethacin", "ketoprofen", "meloxicam",
                            "piroxicam", "celecoxib"),
                    null),
            new DrugGroup("opioid",
                    List.of("codeine", "morphine", "opioid", "opiate", "tramadol", "pethidine"),
                    List.of("codeine", "morphine", "tramadol", "pethidine", "oxycodone", "fentanyl"),
                    null)
    );

    /**
     * Returns one plain-language warning per allergy the medication conflicts
     * with, or an empty list when none do.
     */
    public List<String> findConflicts(String medication, List<Allergy> allergies) {
        List<String> warnings = new ArrayList<>();
        if (medication == null || medication.isBlank() || allergies == null) {
            return warnings;
        }
        String med = normalise(medication);

        for (Allergy allergy : allergies) {
            if (allergy.getAllergen() == null || allergy.getAllergen().isBlank()) {
                continue;
            }
            String allergen = normalise(allergy.getAllergen());
            Set<String> reasons = new LinkedHashSet<>();

            // The allergen named directly in the medication, e.g. "Aspirin 300mg".
            if (med.contains(allergen)) {
                reasons.add("contains " + allergy.getAllergen());
            }
            for (DrugGroup group : GROUPS) {
                boolean allergenInGroup = group.allergenWords().stream().anyMatch(allergen::contains);
                boolean medicationInGroup = group.drugs().stream().anyMatch(med::contains);
                if (allergenInGroup && medicationInGroup) {
                    reasons.add(group.note() != null
                            ? "is a " + group.name() + " (" + group.note() + ")"
                            : "is in the " + group.name() + " group");
                }
            }

            if (!reasons.isEmpty()) {
                String severity = allergy.getSeverity() != null ? allergy.getSeverity().toUpperCase(Locale.ROOT) + " " : "";
                warnings.add(medication.trim() + " " + String.join(" and ", reasons)
                        + " — patient has a " + severity + "allergy to " + allergy.getAllergen()
                        + (allergy.getNotes() != null && !allergy.getNotes().isBlank() ? " (" + allergy.getNotes() + ")" : ""));
            }
        }
        return warnings;
    }

    private static String normalise(String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }
}
