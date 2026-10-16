package com.udhr.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Which names mean the same medicine, and which medicines are in the same
 * class, so stock can be matched however it was typed ("Panado 500mg" is
 * paracetamol) and an out-of-stock medicine can be offered alternatives.
 *
 * A curated list of medicines common in South African primary care, not a full
 * drug database: a medicine it doesn't know still gets an exact stock match,
 * just no alternatives. Classes marked noSubstitute (antiretrovirals, insulin,
 * anticoagulants, anti-epileptics) never get same-class suggestions: switching
 * those is a specialist decision, not a stock decision.
 */
@Component
public class MedicationCatalog {

    public record Medicine(String generic, String drugClass, boolean noSubstitute, List<String> names) {}

    private static final List<Medicine> MEDICINES = List.of(
            // Pain and fever
            med("paracetamol", "analgesic", "paracetamol", "panado", "calpol", "empaped", "tylenol", "acetaminophen"),
            med("ibuprofen", "NSAID", "ibuprofen", "nurofen", "brufen", "advil"),
            med("diclofenac", "NSAID", "diclofenac", "voltaren", "cataflam"),
            med("naproxen", "NSAID", "naproxen", "naprosyn", "aleve"),
            med("mefenamic acid", "NSAID", "mefenamic", "ponstan"),
            // Antibiotics
            med("amoxicillin", "penicillin antibiotic", "amoxicillin", "amoxycillin", "amoxil", "moxypen"),
            med("co-amoxiclav", "penicillin antibiotic", "co-amoxiclav", "coamoxiclav", "augmentin"),
            med("flucloxacillin", "penicillin antibiotic", "flucloxacillin", "floxapen"),
            med("phenoxymethylpenicillin", "penicillin antibiotic", "phenoxymethylpenicillin", "penicillin v", "pen-vee"),
            med("azithromycin", "macrolide antibiotic", "azithromycin", "zithromax"),
            med("erythromycin", "macrolide antibiotic", "erythromycin", "erythrocin"),
            med("clarithromycin", "macrolide antibiotic", "clarithromycin", "klacid"),
            med("cephalexin", "cephalosporin antibiotic", "cephalexin", "cefalexin", "keflex"),
            med("cefuroxime", "cephalosporin antibiotic", "cefuroxime", "zinnat"),
            med("ciprofloxacin", "fluoroquinolone antibiotic", "ciprofloxacin", "ciprobay"),
            med("doxycycline", "tetracycline antibiotic", "doxycycline", "vibramycin"),
            med("metronidazole", "nitroimidazole", "metronidazole", "flagyl"),
            med("co-trimoxazole", "sulfonamide antibiotic", "co-trimoxazole", "cotrimoxazole", "bactrim", "septran"),
            // Diabetes
            med("metformin", "biguanide", "metformin", "glucophage"),
            med("gliclazide", "sulfonylurea", "gliclazide", "diamicron"),
            med("glimepiride", "sulfonylurea", "glimepiride", "amaryl"),
            med("glibenclamide", "sulfonylurea", "glibenclamide", "daonil"),
            noSub("insulin", "insulin", "insulin", "actrapid", "protaphane", "humulin", "lantus", "novorapid"),
            // Blood pressure and heart
            med("amlodipine", "calcium channel blocker", "amlodipine", "norvasc"),
            med("nifedipine", "calcium channel blocker", "nifedipine", "adalat"),
            med("enalapril", "ACE inhibitor", "enalapril", "renitec"),
            med("lisinopril", "ACE inhibitor", "lisinopril", "zestril"),
            med("perindopril", "ACE inhibitor", "perindopril", "coversyl"),
            med("captopril", "ACE inhibitor", "captopril"),
            med("losartan", "angiotensin receptor blocker", "losartan", "cozaar"),
            med("candesartan", "angiotensin receptor blocker", "candesartan", "atacand"),
            med("hydrochlorothiazide", "thiazide diuretic", "hydrochlorothiazide", "hctz"),
            med("indapamide", "thiazide diuretic", "indapamide", "natrilix"),
            med("furosemide", "loop diuretic", "furosemide", "frusemide", "lasix"),
            med("atenolol", "beta blocker", "atenolol", "tenormin"),
            med("carvedilol", "beta blocker", "carvedilol"),
            med("atorvastatin", "statin", "atorvastatin", "lipitor"),
            med("simvastatin", "statin", "simvastatin", "zocor"),
            med("rosuvastatin", "statin", "rosuvastatin", "crestor"),
            noSub("warfarin", "anticoagulant", "warfarin", "coumadin"),
            med("aspirin", "antiplatelet", "aspirin", "disprin", "ecotrin"),
            // Breathing and allergy
            med("salbutamol", "short-acting bronchodilator", "salbutamol", "ventolin", "asthavent"),
            med("budesonide", "inhaled steroid", "budesonide", "pulmicort"),
            med("beclomethasone", "inhaled steroid", "beclomethasone", "clenil", "qvar"),
            med("loratadine", "antihistamine", "loratadine", "clarityne"),
            med("cetirizine", "antihistamine", "cetirizine", "zyrtec"),
            med("fexofenadine", "antihistamine", "fexofenadine", "telfast", "fexo"),
            med("chlorphenamine", "antihistamine", "chlorphenamine", "chlorpheniramine", "allergex"),
            med("prednisone", "oral steroid", "prednisone", "prednisolone"),
            // Stomach
            med("omeprazole", "proton pump inhibitor", "omeprazole", "losec"),
            med("pantoprazole", "proton pump inhibitor", "pantoprazole", "pantoloc"),
            med("lansoprazole", "proton pump inhibitor", "lansoprazole"),
            med("metoclopramide", "anti-emetic", "metoclopramide", "maxolon"),
            med("ondansetron", "anti-emetic", "ondansetron", "zofran"),
            med("loperamide", "anti-diarrhoeal", "loperamide", "imodium"),
            // Infections
            med("fluconazole", "antifungal", "fluconazole", "diflucan"),
            med("albendazole", "anti-worm", "albendazole", "zentel"),
            med("mebendazole", "anti-worm", "mebendazole", "vermox"),
            // One entry per regimen or drug: two different ARVs are never "the same medicine".
            noSub("tenofovir/lamivudine/dolutegravir", "antiretroviral", "tld", "tenofovir/lamivudine/dolutegravir"),
            noSub("tenofovir/emtricitabine/efavirenz", "antiretroviral", "atripla", "tenofovir/emtricitabine/efavirenz"),
            noSub("efavirenz", "antiretroviral", "efavirenz", "stocrin"),
            noSub("nevirapine", "antiretroviral", "nevirapine", "viramune"),
            noSub("zidovudine", "antiretroviral", "zidovudine"),
            noSub("abacavir", "antiretroviral", "abacavir"),
            noSub("lopinavir/ritonavir", "antiretroviral", "lopinavir", "aluvia", "kaletra"),
            noSub("rifampicin/isoniazid/pyrazinamide/ethambutol", "TB treatment", "rifafour", "rhze"),
            noSub("isoniazid", "TB treatment", "isoniazid"),
            noSub("rifampicin", "TB treatment", "rifampicin"),
            // Mental health and seizures
            med("fluoxetine", "SSRI antidepressant", "fluoxetine", "prozac"),
            med("sertraline", "SSRI antidepressant", "sertraline", "zoloft"),
            med("citalopram", "SSRI antidepressant", "citalopram", "cipramil"),
            noSub("carbamazepine", "anti-epileptic", "carbamazepine", "tegretol"),
            noSub("sodium valproate", "anti-epileptic", "valproate", "epilim"),
            noSub("phenytoin", "anti-epileptic", "phenytoin", "epanutin")
    );

    private static Medicine med(String generic, String drugClass, String... names) {
        return new Medicine(generic, drugClass, false, List.of(names));
    }

    private static Medicine noSub(String generic, String drugClass, String... names) {
        return new Medicine(generic, drugClass, true, List.of(names));
    }

    /** The medicine a typed name refers to, e.g. "Panado 500mg" → paracetamol. */
    public Optional<Medicine> identify(String typed) {
        if (typed == null || typed.isBlank()) {
            return Optional.empty();
        }
        String text = typed.toLowerCase(Locale.ROOT);
        return MEDICINES.stream()
                .filter(m -> m.names().stream().anyMatch(name -> containsWord(text, name)))
                .findFirst();
    }

    // Whole words only, so "fexo" doesn't match "fexofenadine" and a short
    // name can't match inside another word ("Ventolin inhaler").
    private static boolean containsWord(String text, String name) {
        return Pattern.compile("(^|[^a-z])" + Pattern.quote(name) + "(?![a-z])").matcher(text).find();
    }
}
