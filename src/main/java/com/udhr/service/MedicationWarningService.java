package com.udhr.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * The "medicines to avoid" a patient is shown for each of their allergies
 * (patient portal and health assistant).
 *
 * This used to search the US FDA drug-label database for any label that
 * mentioned the allergen. On the live server that returned nonsense: "Unknown
 * Generic (Unknown Medication)", and lidocaine or cidofovir for a sulfa
 * allergy, simply because their labels mention sulfa, while never naming
 * amoxicillin for a penicillin allergy. A patient told to avoid the wrong
 * medicines is worse off than one told nothing, so the list is now curated,
 * and uses the same drug groups as the prescribing check (AllergyCheckService).
 */
@Service
public class MedicationWarningService {

    public List<Map<String, Object>> getMedicationWarnings(String allergen) {
        if (allergen == null || allergen.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = allergen.toLowerCase();

        if (containsAny(normalized, "penicillin", "amoxicillin", "ampicillin", "augmentin", "beta-lactam", "beta lactam")) {
            return List.of(warning(
                    "Amoxicillin, Co-amoxiclav, Ampicillin, Flucloxacillin, Penicillin V",
                    "Amoxil, Moxypen, Augmentin, Pen-Vee K",
                    "These are all penicillins. Cephalosporins such as Cephalexin can also react: check with your doctor before taking one. "
                            + "Alternatives such as Azithromycin or Erythromycin may be prescribed instead."));
        }
        if (containsAny(normalized, "sulfa", "sulpha", "sulfonamide", "sulphonamide", "co-trimoxazole", "cotrimoxazole", "bactrim")) {
            return List.of(warning(
                    "Co-trimoxazole (Sulfamethoxazole-Trimethoprim), Sulfasalazine, Sulfadiazine",
                    "Bactrim, Septran",
                    "Risk of severe skin reactions (Stevens-Johnson syndrome). Tell your doctor before taking water pills (thiazides) or diabetes tablets called sulfonylureas."));
        }
        if (containsAny(normalized, "aspirin", "nsaid", "ibuprofen", "diclofenac", "naproxen", "anti-inflammator")) {
            return List.of(warning(
                    "Aspirin, Ibuprofen, Diclofenac, Naproxen, Mefenamic acid",
                    "Disprin, Ecotrin, Nurofen, Brufen, Voltaren, Ponstan",
                    "These anti-inflammatory painkillers can cause wheezing or hives in sensitive patients. Paracetamol is usually used for mild pain instead."));
        }
        if (containsAny(normalized, "codeine", "morphine", "opioid", "opiate", "tramadol", "pethidine")) {
            return List.of(warning(
                    "Codeine, Morphine, Tramadol, Pethidine, Oxycodone",
                    "Found in many cough mixtures and strong painkillers",
                    "Check the label of any cough or pain medicine, and ask your pharmacist before taking it."));
        }
        return List.of(warning(
                "Any medicine containing " + allergen,
                "Check the label",
                "Tell every doctor and pharmacist about this allergy before starting a new medicine, and watch for hives, swelling or trouble breathing."));
    }

    private static Map<String, Object> warning(String genericName, String brandName, String warningText) {
        Map<String, Object> w = new HashMap<>();
        w.put("genericName", genericName);
        w.put("brandName", brandName);
        w.put("warningText", warningText);
        return w;
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
