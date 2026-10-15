package com.udhr.service;

import com.udhr.dto.ChatbotResponse;
import com.udhr.dto.HealthGuidanceResponse;
import com.udhr.dto.IngredientCheckResult;
import com.udhr.model.DietaryGuideline;
import com.udhr.model.HealthTip;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Symptom;
import com.udhr.model.SymptomCheck;
import com.udhr.repository.AllergyRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.PrescriptionRepository;
import com.udhr.repository.SymptomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private SymptomRepository symptomRepository;

    @Autowired
    private SymptomCheckerService symptomCheckerService;

    @Autowired
    private HealthGuidanceService healthGuidanceService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private FoodIngredientService foodIngredientService;

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private AllergyCheckService allergyCheckService;

    // Matches "can I eat X", "is it safe to eat X", "should I eat X" — the
    // captured group is looked up against the same ingredient/allergy database
    // the Food Checker feature uses, not a hardcoded answer.
    private static final Pattern FOOD_SAFETY_PATTERN = Pattern.compile(
            "(?:can i (?:eat|have)|is it safe to eat|is safe to eat|should i eat) (.+)");

    // Checked before anything else, on every message. A person in crisis must
    // never get a cheerful greeting or "I didn't catch that" — both happened
    // before ("I'm thinking about suicide" matched the greeting check).
    private static final List<String> SELF_HARM_PHRASES = List.of(
            "suicide", "suicidal", "kill myself", "killing myself", "end my life", "end it all",
            "take my own life", "want to die", "wanna die", "better off dead", "don't want to live",
            "dont want to live", "don't want to be alive", "no reason to live", "hurt myself",
            "harm myself", "self harm", "self-harm", "cut myself", "cutting myself", "overdose",
            "take all my pills"
    );

    // Phrases that warrant an immediate emergency response, independent of the
    // seeded symptom list (which only covers Kaggle disease-symptom wording).
    private static final List<String> EMERGENCY_PHRASES = List.of(
            "can't breathe", "cant breathe", "cannot breathe", "difficulty breathing", "not breathing",
            "choking", "severe bleeding", "bleeding a lot", "heavy bleeding", "won't stop bleeding",
            "wont stop bleeding", "unconscious", "unresponsive", "collapsed", "passed out",
            "won't wake up", "wont wake up", "seizure", "fitting", "convulsion",
            "heart attack", "stroke", "face drooping", "crushing chest", "chest pain",
            "throat is closing", "throat swelling", "anaphylaxis", "poisoned", "snake bite", "snakebite"
    );

    private static final String CRISIS_REPLY =
            "I'm really sorry you're feeling this way, and I'm glad you said something. You don't have to go through this alone.\n\n"
            + "If you are in danger right now, call 10177 for an ambulance (112 from a cell phone) or go to your nearest emergency unit.\n\n"
            + "You can talk to someone right now at the SADAG Suicide Crisis Helpline: 0800 567 567 (free, 24 hours).\n\n"
            + "If you can, tell someone you trust, or the staff at your clinic, how you are feeling.";

    private static final String EMERGENCY_REPLY =
            "This sounds like it could be a medical emergency. Call 10177 for an ambulance (112 from a cell phone) "
            + "or go to the nearest emergency department right away. If someone is with you, ask them to help.";

    // A message that is only a greeting. Whole-word and whole-message: the old
    // check matched "hi" inside any word (which, this, child, HIV), so real
    // questions got "Hi! I'm your health assistant" back.
    private static final Pattern GREETING_ONLY = Pattern.compile(
            "^\\s*(?:hi|hello|hey|hiya|howzit|sawubona|dumela|molo|good (?:morning|afternoon|evening))"
            + "(?:\\s+(?:there|doc|doctor|assistant))?[\\s!.,?]*$");

    // "Can I take ibuprofen with my metformin?", "can I drink alcohol with...",
    // "do these interact?" — questions the assistant can't answer safely itself.
    private static final Pattern INTERACTION_QUESTION = Pattern.compile(
            "\\b(?:can|may|should)\\s+i\\s+(?:take|use|mix|combine|drink)\\b.*\\b(?:with|and|together|while|on)\\b"
            + "|\\binteract|\\bmix(?:ing)?\\s+(?:my\\s+)?(?:medicine|medication|pills|tablets)");

    // Words that cancel a symptom mentioned shortly after them: "I don't have a
    // fever" must not be triaged as a fever.
    private static final Pattern NEGATION = Pattern.compile(
            "\\b(?:no|not|don't|dont|do not|never|without|haven't|havent|hasn't|hasnt|isn't|isnt|no longer|denies)\\b");

    // Colloquial phrasing mapped to the exact seeded Symptom.name it corresponds to,
    // so free-text messages match the same symptom set the checkbox-based checker uses.
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("Vomiting", List.of("throwing up", "throw up", "puking", "vomit")),
            Map.entry("Breathlessness", List.of("short of breath", "shortness of breath", "trouble breathing", "breathless")),
            Map.entry("Stomach Pain", List.of("stomach ache", "stomach hurts", "my stomach")),
            Map.entry("Abdominal Pain", List.of("tummy ache", "tummy hurts", "belly ache")),
            Map.entry("Throat Irritation", List.of("sore throat", "throat hurts")),
            Map.entry("High Fever", List.of("high temperature", "burning up")),
            // Not plain "temperature": "what temperature should I store my insulin
            // at?" was triaged as a fever.
            Map.entry("Mild Fever", List.of("fever", "have a temperature", "running a temperature", "feverish")),
            Map.entry("Fatigue", List.of("tired", "exhausted", "no energy", "worn out")),
            Map.entry("Dizziness", List.of("dizzy", "light headed", "lightheaded")),
            Map.entry("Nausea", List.of("nauseous", "feel sick", "queasy")),
            Map.entry("Itching", List.of("itchy")),
            Map.entry("Skin Rash", List.of("rash")),
            Map.entry("Runny Nose", List.of("sniffles")),
            Map.entry("Cold Hands And Feets", List.of("cold hands", "cold feet"))
    );

    public ChatbotResponse respond(String idNumber, String rawMessage) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) {
            return new ChatbotResponse(
                    "Tell me what you're feeling, or ask me about your medications, allergies, or diet.",
                    null, null);
        }
        String lower = message.toLowerCase();

        // Safety first, before any other matching, and never negation-filtered:
        // "I'm not suicidal but..." still gets the support numbers.
        if (SELF_HARM_PHRASES.stream().anyMatch(lower::contains)) {
            return new ChatbotResponse(CRISIS_REPLY, "RED", null);
        }
        if (EMERGENCY_PHRASES.stream().anyMatch(lower::contains)
                || (containsAny(lower, "pregnan") && containsAny(lower, "bleed"))) {
            return new ChatbotResponse(EMERGENCY_REPLY, "RED", null);
        }

        if (GREETING_ONLY.matcher(lower).matches()) {
            return new ChatbotResponse(
                    "Hi! I'm your health assistant. Describe any symptoms you're feeling (e.g. \"I have a headache and fever\") and I'll guide you on next steps, or ask me about your medications, allergies, or diet.",
                    null, null);
        }

        if (INTERACTION_QUESTION.matcher(lower).find()) {
            return new ChatbotResponse(buildInteractionReply(patient, message), null, null);
        }

        Set<Symptom> matched = new LinkedHashSet<>();
        for (Symptom symptom : symptomRepository.findAll()) {
            boolean hit = mentioned(lower, symptom.getName().toLowerCase());
            if (!hit) {
                for (String synonym : SYNONYMS.getOrDefault(symptom.getName(), List.of())) {
                    if (mentioned(lower, synonym)) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) {
                matched.add(symptom);
            }
        }

        if (!matched.isEmpty()) {
            List<Long> symptomIds = matched.stream().map(Symptom::getId).collect(Collectors.toList());
            List<String> symptomNames = matched.stream().map(Symptom::getName).collect(Collectors.toList());
            SymptomCheck check = symptomCheckerService.performSymptomCheck(idNumber, symptomIds);
            String reply = "I picked up on: " + String.join(", ", symptomNames) + ".\n\n"
                    + check.getRecommendation().split("\\[")[0].trim();
            return new ChatbotResponse(reply, check.getUrgencyLevel(), symptomNames);
        }

        Matcher foodMatcher = FOOD_SAFETY_PATTERN.matcher(lower);
        if (foodMatcher.find()) {
            String food = foodMatcher.group(1).replaceAll("[?.!]+$", "").trim();
            if (!food.isEmpty()) {
                return new ChatbotResponse(buildFoodSafetyReply(idNumber, food), null, null);
            }
        }

        // Check the more specific "which drugs should I avoid" (allergy-based FDA
        // warnings) before the generic diet-avoid and current-medications intents,
        // since both of those also match on "avoid" / "medication".
        if (containsAny(lower, "medication", "medicine", "drug", "pill") && containsAny(lower, "avoid")) {
            return new ChatbotResponse(buildMedicationAvoidReply(idNumber), null, null);
        }

        if (containsAny(lower, "diet", "eat", "food guideline", "avoid")) {
            return new ChatbotResponse(buildDietReply(idNumber), null, null);
        }

        if (containsAny(lower, "medication", "medicine", "prescription", "pills", "dose")) {
            return new ChatbotResponse(buildMedicationReply(patient), null, null);
        }

        if (containsAny(lower, "allerg")) {
            return new ChatbotResponse(buildAllergyReply(idNumber), null, null);
        }

        return new ChatbotResponse(
                "I didn't catch a specific symptom or question there. Try describing how you feel, e.g. \"I have a cough and fever\", or ask about your medications, allergies, or diet.",
                null, null);
    }

    // Each needle must start a word: "eat" matches "eat" and "eating" but not
    // "breathe" or "sweat"; "pill" matches "pills" but not "spill".
    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (Pattern.compile("\\b" + Pattern.quote(needle)).matcher(haystack).find()) {
                return true;
            }
        }
        return false;
    }

    // True if the term appears as a word and at least once isn't negated in the
    // same clause just before it ("no fever", "I don't have a headache").
    private boolean mentioned(String text, String term) {
        Matcher m = Pattern.compile("\\b" + Pattern.quote(term)).matcher(text);
        while (m.find()) {
            String before = text.substring(Math.max(0, m.start() - 30), m.start());
            int clauseStart = Math.max(
                    Math.max(before.lastIndexOf('.'), before.lastIndexOf(',')),
                    Math.max(Math.max(before.lastIndexOf(';'), before.lastIndexOf('!')),
                            Math.max(before.lastIndexOf('?'), before.lastIndexOf(" but "))));
            String clause = clauseStart >= 0 ? before.substring(clauseStart + 1) : before;
            if (!NEGATION.matcher(clause).find()) {
                return true;
            }
        }
        return false;
    }

    private String buildInteractionReply(Patient patient, String message) {
        StringBuilder sb = new StringBuilder(
                "I can't safely check whether medicines can be taken together. Please ask your pharmacist or doctor "
                + "before combining them, including over-the-counter painkillers, herbal or traditional remedies, and alcohol.");

        // If the message names something the patient is allergic to, say so.
        List<String> allergyWarnings = allergyCheckService.findConflicts(message, allergyRepository.findByPatientId(patient.getId()));
        if (!allergyWarnings.isEmpty()) {
            sb.append("\n\n⚠️ Important: your record shows an allergy that may apply here. Do not take it without speaking to your doctor or pharmacist first.");
        }

        List<Prescription> active = prescriptionRepository.findByPatientIdAndActiveTrue(patient.getId());
        if (!active.isEmpty()) {
            sb.append("\n\nYour current medications, to show them:");
            for (Prescription p : active) {
                sb.append("\n- ").append(p.getMedication()).append(" (").append(p.getDosage()).append(")");
            }
        }
        return sb.toString();
    }

    private String buildFoodSafetyReply(String idNumber, String food) {
        List<IngredientCheckResult> results = foodIngredientService.checkIngredients(idNumber, food);
        if (results.isEmpty()) {
            return "I couldn't check \"" + food + "\" — try naming a specific ingredient, e.g. \"can I eat sugar?\"";
        }
        return results.get(0).getReason();
    }

    private String buildDietReply(String idNumber) {
        HealthGuidanceResponse guidance = healthGuidanceService.getPersonalGuidance(idNumber);
        List<DietaryGuideline> guidelines = guidance.getDietaryGuidelines();
        List<HealthTip> tips = guidance.getHealthTips();
        if ((guidelines == null || guidelines.isEmpty()) && (tips == null || tips.isEmpty())) {
            return "You don't have any specific dietary guidelines on file yet — this is usually based on chronic conditions your doctor has recorded. Ask your doctor for personalized dietary advice.";
        }
        StringBuilder sb = new StringBuilder("Based on your conditions (" + String.join(", ", guidance.getConditions()) + "), here's what's recommended:\n");
        if (guidelines != null) {
            for (DietaryGuideline g : guidelines) {
                sb.append(g.getFoodType().equals("EAT") ? "\n✓ Eat: " : "\n✕ Avoid: ").append(g.getFoodItem());
                if (g.getDescription() != null && !g.getDescription().isBlank()) {
                    sb.append(" — ").append(g.getDescription());
                }
            }
        }
        if (tips != null && !tips.isEmpty()) {
            sb.append("\n\nLifestyle tips:");
            for (HealthTip tip : tips) {
                sb.append("\n• ").append(tip.getTitle());
                if (tip.getDescription() != null && !tip.getDescription().isBlank()) {
                    sb.append(" — ").append(tip.getDescription());
                }
            }
        }
        return sb.toString();
    }

    private String buildMedicationReply(Patient patient) {
        List<Prescription> active = prescriptionRepository.findByPatientIdAndActiveTrue(patient.getId());
        if (active.isEmpty()) {
            return "You don't have any active prescriptions on file right now.";
        }
        StringBuilder sb = new StringBuilder("Here are your active medications:\n");
        for (Prescription p : active) {
            sb.append("\n- ").append(p.getMedication()).append(" (").append(p.getDosage()).append("), ")
                    .append(p.getFrequency()).append(", until ").append(p.getEndDate());
        }
        return sb.toString();
    }

    private String buildMedicationAvoidReply(String idNumber) {
        HealthGuidanceResponse guidance = healthGuidanceService.getPersonalGuidance(idNumber);
        Map<String, List<Map<String, Object>>> warnings = guidance.getMedicationWarnings();
        if (warnings == null || warnings.isEmpty()) {
            return "You don't have any recorded drug allergies, so there are no specific medications flagged to avoid. Always tell your doctor or pharmacist about any allergy before starting something new.";
        }
        StringBuilder sb = new StringBuilder("Based on your allergies, here's what to avoid:\n");
        boolean foundAny = false;
        for (Map.Entry<String, List<Map<String, Object>>> entry : warnings.entrySet()) {
            List<Map<String, Object>> drugWarnings = entry.getValue();
            if (drugWarnings == null) continue;
            for (Map<String, Object> w : drugWarnings) {
                foundAny = true;
                sb.append("\n⚠️ Avoid ").append(w.get("genericName")).append(" (").append(w.get("brandName"))
                        .append(") — allergic to ").append(entry.getKey());
            }
        }
        if (!foundAny) {
            return "You have a recorded allergy to " + String.join(", ", warnings.keySet())
                    + ", but no specific medication warnings on file for it. Always tell your pharmacist about this allergy before taking anything new.";
        }
        return sb.toString();
    }

    private String buildAllergyReply(String idNumber) {
        HealthGuidanceResponse guidance = healthGuidanceService.getPersonalGuidance(idNumber);
        List<String> allergies = guidance.getAllergies();
        if (allergies == null || allergies.isEmpty()) {
            return "You don't have any allergies on file.";
        }
        return "Your recorded allergies: " + String.join(", ", allergies) + ". Always tell any new doctor or pharmacist about these before taking new medication.";
    }
}
