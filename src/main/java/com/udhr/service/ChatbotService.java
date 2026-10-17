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
import java.util.ArrayList;
import java.util.Comparator;
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

    @Autowired
    private SymptomAdvice symptomAdvice;

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
            "take all my pills", "lethal dose", "fatal dose"
    );

    // "How many paracetamol would kill me?" asks for an overdose amount without
    // saying "overdose": it gets the crisis reply, never a number.
    private static final Pattern LETHAL_AMOUNT = Pattern.compile(
            "\\bhow (?:many|much)\\b.{0,50}\\b(?:die|dying|kill|fatal|lethal|dead|death)\\b");
    private static final Pattern MEDICINE_WORD = Pattern.compile(
            "\\b(?:pills?|tablets?|paracetamol|panado|medicines?|medications?|meds|insulin|sleeping|drugs?|poison)\\b");

    private static final Pattern HARM_OTHERS = Pattern.compile(
            "\\b(?:kill|poison|hurt|harm|stab|drug)\\s+(?:someone|somebody|a person|people|him|her|them|"
            + "my\\s+(?:wife|husband|partner|boyfriend|girlfriend|mother|mom|mum|father|dad|child|son|daughter|baby|"
            + "friend|neighbou?r|boss|brother|sister|teacher))\\b");

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

    private static final String HARM_OTHERS_REPLY =
            "I can't help with harming anyone. If someone is in danger right now, call 10111 (police) or 10177 (ambulance).\n\n"
            + "If you are afraid you might hurt someone, please talk to someone now: the SADAG helpline is free, 24 hours, on 0800 567 567.";

    private static final String EMERGENCY_REPLY =
            "This sounds like it could be a medical emergency. Call 10177 for an ambulance (112 from a cell phone) "
            + "or go to the nearest emergency department right away. If someone is with you, ask them to help.";

    private static final String SCOPE_REPLY =
            "I'm the clinic's health assistant, so I can only help with health: how you're feeling, your medicines, "
            + "allergies and diet. For anything else, please ask the staff at your clinic.";

    private static final String IDENTITY_REPLY =
            "I'm the UDHR health assistant: a computer program, not a person or a doctor. I can:\n"
            + "• suggest what to do about common symptoms, and when to go to the clinic\n"
            + "• show your medicines and allergies\n"
            + "• check foods against your allergies and conditions\n"
            + "I can't diagnose you or prescribe medicine. In an emergency, call 10177 (112 from a cell phone).";

    private static final String FALLBACK_REPLY =
            "I'm not sure I understood that. I can help with:\n"
            + "• how you're feeling, e.g. \"I've had a bad headache since yesterday\"\n"
            + "• your medicines, allergies and diet, e.g. \"what medicines am I on?\"\n"
            + "For anything else, please ask the staff at your clinic.";

    // A message that is only a greeting. Whole-word and whole-message: the old
    // check matched "hi" inside any word (which, this, child, HIV), so real
    // questions got "Hi! I'm your health assistant" back.
    private static final Pattern GREETING_ONLY = Pattern.compile(
            "^\\s*(?:hi|hello|hey|hiya|howzit|sawubona|dumela|molo|good (?:morning|afternoon|evening))"
            + "(?:\\s+(?:there|doc|doctor|assistant))?[\\s!.,?]*$");

    private static final Pattern IDENTITY = Pattern.compile(
            "\\b(?:who|what) are you\\b|\\bwhat can you (?:do|help)|\\bhow do you work\\b"
            + "|\\bare you (?:a |an )?(?:human|person|robot|bot|ai|real|doctor|nurse)\\b");

    // Questions a health assistant has no business answering. Built to miss
    // health wording: no "/" (blood pressure is 120/80) and no "-" (3-4 days).
    private static final Pattern OFF_TOPIC = Pattern.compile(
            "\\bjokes?\\b|\\briddle\\b|\\bweather\\b|\\bcapital of\\b|\\bpresident\\b|\\bsoccer\\b|\\bfootball\\b"
            + "|\\bbitcoin\\b|\\bcrypto\\b|\\blotto\\b|\\bhomework\\b|\\bassignment\\b|\\bmovie\\b"
            + "|\\b(?:write|compose)\\b.{0,15}\\b(?:poem|essay|story|song|letter)\\b"
            + "|\\d+\\s*[+*×]\\s*\\d+|\\d+\\s+x\\s+\\d+|\\bwhat is \\d+\\s*(?:plus|minus|times|divided)"
            + "|\\bwhat time is it\\b|\\bwho (?:won|is the)\\b|\\bignore (?:your|all|previous)\\b|\\bpretend\\b|\\byou are now\\b");

    // "Can I take ibuprofen with my metformin?", "can I drink alcohol with...",
    // "do these interact?" — questions the assistant can't answer safely itself.
    private static final Pattern INTERACTION_QUESTION = Pattern.compile(
            "\\b(?:can|may|should)\\s+i\\s+(?:take|use|mix|combine|drink)\\b.*\\b(?:with|and|together|while|on)\\b"
            + "|\\binteract|\\bmix(?:ing)?\\s+(?:my\\s+)?(?:medicine|medication|pills|tablets)");

    // Changing treatment on one's own: answered with why not, never with "yes".
    private static final String MEDS = "(?:medicines?|medications?|meds|pills|tablets|treatment|arvs?|insulin|antibiotics?|tb treatment|dose)";
    private static final Pattern MISSED_DOSE = Pattern.compile(
            "\\b(?:missed|miss|forgot|forget|forgotten)\\b.{0,30}\\b(?:dose|doses|pills?|tablets?|medicines?|medications?|meds|arvs?|insulin)\\b");
    private static final Pattern STOP_MEDICINE = Pattern.compile(
            "\\b(?:stop|stopping|quit|quitting|skip|skipping|give up|come off)\\b.{0,30}\\b" + MEDS + "\\b");
    private static final Pattern MORE_MEDICINE = Pattern.compile(
            "\\b(?:double|extra|more|two|2|three|3)\\b.{0,20}\\b(?:dose|doses|pills|tablets)\\b|\\btake more than\\b");
    private static final Pattern ASKING = Pattern.compile(
            "\\b(?:can|should|may|could) i\\b|\\bis it (?:ok|okay|safe|fine)\\b|\\bwhat if i\\b|\\bwhat happens if\\b");
    private static final Pattern ANTIBIOTIC_REQUEST = Pattern.compile(
            "\\b(?:need|want|give me|get|buy|prescribe|have)\\b.{0,20}\\bantibiotics?\\b");
    private static final Pattern SOMEONE_ELSES_MEDICINE = Pattern.compile(
            "\\b(?:someone else'?s|somebody else'?s|my (?:friend|sister|brother|mother|mom|mum|father|dad|neighbou?r|husband|wife|partner|child)'?s)\\s+"
            + "(?:medicines?|medications?|meds|pills|tablets|arvs?|insulin|inhaler)");
    private static final Pattern OTHER_PEOPLES_RECORDS = Pattern.compile(
            "\\b(?:another patient|other patients?|someone else'?s|somebody else'?s|"
            + "my (?:friend|neighbou?r|husband|wife|partner|sister|brother)'?s)\\b.{0,20}\\b(?:records?|file|results|details|information|info|history)\\b");

    private static final Pattern SEVERE = Pattern.compile(
            "\\b(?:strong|stron|severe|very bad|really bad|so bad|terrible|unbearable|excruciating|worst|killing me|a lot of pain|so much pain)\\b");

    private static final Pattern CONDITION_QUESTION = Pattern.compile(
            "\\b(?:what is|what's|whats|tell me about|explain|information on|info on|about)\\b"
            + "|\\bhow (?:do|can|to|should) (?:i )?(?:manage|control|lower|reduce|live with|prevent|treat|cope with)\\b");

    // Short answers to "what is diabetes?" and "how do I manage my blood
    // pressure?" for the long-term conditions most clinic patients have.
    private static final Map<Pattern, String> CONDITION_INFO = Map.of(
            Pattern.compile("\\bdiabet"),
            "Diabetes means your blood sugar runs too high. To manage it:\n"
            + "• take your tablets or insulin every day as prescribed\n"
            + "• eat regular meals with vegetables and whole grains, and cut down on sugar, sweet drinks and white bread\n"
            + "• walk or exercise for 30 minutes on most days\n"
            + "• check your feet every day for sores, and go to every clinic check-up\n"
            + "Go to the clinic urgently if you are very thirsty, passing a lot of urine, vomiting or confused.",
            Pattern.compile("\\bhigh blood pressure\\b|\\bhypertension\\b|\\bblood pressure\\b"),
            "High blood pressure often has no symptoms, but it strains your heart, brain and kidneys. To manage it:\n"
            + "• take your tablets every day, even when you feel well\n"
            + "• use less salt, stock cubes, processed meat and chips\n"
            + "• be active for 30 minutes on most days and keep a healthy weight\n"
            + "• limit alcohol and don't smoke\n"
            + "A sudden severe headache, chest pain or weakness on one side is an emergency: call 10177.",
            Pattern.compile("\\bhiv\\b|\\baids\\b"),
            "HIV is a virus that weakens the immune system. With ARVs taken every day, people with HIV live long, healthy lives, "
            + "and an undetectable viral load means HIV isn't passed on through sex.\n"
            + "• take your ARVs at the same time every day, and never stop them on your own\n"
            + "• keep your clinic visits for viral load tests\n"
            + "Testing and treatment are free at government clinics.",
            Pattern.compile("\\btb\\b|\\btuberculosis\\b"),
            "TB is a bacterial infection, usually of the lungs, spread by coughing. Signs: a cough for more than 2 weeks, fever, "
            + "night sweats and weight loss. TB is curable with free treatment from the clinic, but you must finish the whole course "
            + "(usually 6 months) even when you feel better.",
            Pattern.compile("\\basthma"),
            "Asthma narrows the airways, causing wheezing, coughing and shortness of breath.\n"
            + "• use your preventer inhaler every day as prescribed, and carry your reliever (blue) inhaler\n"
            + "• avoid smoke, dust and anything else that sets it off\n"
            + "If your reliever doesn't help, or you can't speak in full sentences, it's an emergency: call 10177.");

    // Words that cancel a symptom mentioned shortly after them: "I don't have a
    // fever" must not be triaged as a fever.
    private static final Pattern NEGATION = Pattern.compile(
            "\\b(?:no|not|don't|dont|do not|never|without|haven't|havent|hasn't|hasnt|isn't|isnt|no longer|denies)\\b");

    private static final Pattern WORD = Pattern.compile("[a-z']+");

    // Colloquial phrasing mapped to the exact seeded Symptom.name it corresponds to,
    // so free-text messages match the same symptom set the checkbox-based checker uses.
    // Misspellings of words too short to fuzzy-match safely are listed here too.
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("Headache", List.of("head hurts", "head is sore", "sore head", "head pain", "head is killing me",
                    "head ache", "migraine", "my head is pounding")),
            Map.entry("Cough", List.of("coughing", "coff", "cof", "cought", "caugh")),
            Map.entry("Diarrhoea", List.of("diarrhea", "runny tummy", "running stomach", "loose stool", "loose stools", "running tummy")),
            Map.entry("Back Pain", List.of("back hurts", "sore back", "backache", "back ache", "lower back")),
            Map.entry("Vomiting", List.of("throwing up", "throw up", "puking", "vomit", "vomited")),
            Map.entry("Breathlessness", List.of("short of breath", "shortness of breath", "trouble breathing", "breathless")),
            Map.entry("Stomach Pain", List.of("stomach ache", "stomach hurts", "my stomach", "stomach cramps")),
            Map.entry("Abdominal Pain", List.of("tummy ache", "tummy hurts", "belly ache", "sore tummy")),
            Map.entry("Throat Irritation", List.of("sore throat", "throat hurts", "throat is sore", "painful throat")),
            Map.entry("High Fever", List.of("high temperature", "burning up")),
            // Not plain "temperature": "what temperature should I store my insulin
            // at?" was triaged as a fever.
            Map.entry("Mild Fever", List.of("fever", "fevr", "have a temperature", "running a temperature", "feverish", "hot body")),
            Map.entry("Fatigue", List.of("tired", "exhausted", "no energy", "worn out")),
            Map.entry("Dizziness", List.of("dizzy", "light headed", "lightheaded")),
            Map.entry("Nausea", List.of("nauseous", "feel sick", "queasy")),
            Map.entry("Itching", List.of("itchy")),
            Map.entry("Skin Rash", List.of("rash")),
            Map.entry("Runny Nose", List.of("sniffles", "flu", "have a cold", "got a cold", "blocked nose")),
            Map.entry("Cold Hands And Feets", List.of("cold hands", "cold feet"))
    );

    // At most this many symptom guides in one reply, so it stays readable.
    private static final int MAX_GUIDES = 2;

    private static final List<String> URGENCY_ORDER = List.of("GREEN", "YELLOW", "RED");

    /** One answer to one part of a message; a long message can have several. */
    private record Part(String text, String urgency) {}

    public ChatbotResponse respond(String idNumber, String rawMessage) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) {
            return new ChatbotResponse(
                    "Tell me what you're feeling, or ask me about your medications, allergies, or diet.",
                    null, null);
        }
        String lower = message.toLowerCase().replace('’', '\'');

        // Safety first, before any other matching, and never negation-filtered:
        // "I'm not suicidal but..." still gets the support numbers. These replies
        // stand alone: nothing else in the message is answered alongside them.
        if (SELF_HARM_PHRASES.stream().anyMatch(lower::contains)
                || (LETHAL_AMOUNT.matcher(lower).find() && MEDICINE_WORD.matcher(lower).find())) {
            return new ChatbotResponse(CRISIS_REPLY, "RED", null);
        }
        if (HARM_OTHERS.matcher(lower).find()) {
            return new ChatbotResponse(HARM_OTHERS_REPLY, "RED", null);
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
        if (IDENTITY.matcher(lower).find()) {
            return new ChatbotResponse(IDENTITY_REPLY, null, null);
        }

        boolean offTopic = OFF_TOPIC.matcher(lower).find();

        // Everything below can apply to the same message ("I have a cough and a
        // headache, and what am I allergic to?"), so each part is answered.
        List<Part> parts = new ArrayList<>();
        List<String> symptomNames = null;

        Part medicineSafety = medicineSafetyPart(patient, lower);
        if (medicineSafety != null) {
            parts.add(medicineSafety);
        }

        boolean interaction = INTERACTION_QUESTION.matcher(lower).find();
        if (interaction && medicineSafety == null) {
            parts.add(new Part(buildInteractionReply(patient, message), null));
        }

        Set<Symptom> matched = matchSymptoms(lower);
        if (!matched.isEmpty()) {
            symptomNames = matched.stream().map(Symptom::getName).collect(Collectors.toList());
            parts.add(symptomPart(patient, idNumber, lower, matched));
        }

        if (CONDITION_QUESTION.matcher(lower).find()) {
            for (Map.Entry<Pattern, String> condition : CONDITION_INFO.entrySet()) {
                if (condition.getKey().matcher(lower).find()) {
                    parts.add(new Part(condition.getValue(), null));
                }
            }
        }

        Matcher foodMatcher = FOOD_SAFETY_PATTERN.matcher(lower);
        if (foodMatcher.find()) {
            String food = foodMatcher.group(1).replaceAll("[?.!]+$", "").trim();
            if (!food.isEmpty()) {
                parts.add(new Part(buildFoodSafetyReply(idNumber, food), null));
            }
        }

        // The more specific "which drugs should I avoid" (allergy-based warnings)
        // wins over the generic diet-avoid and current-medications intents,
        // since both of those also match on "avoid" / "medication".
        boolean medicationAvoid = containsAny(lower, "medication", "medicine", "drug", "pill") && containsAny(lower, "avoid");
        if (medicationAvoid) {
            parts.add(new Part(buildMedicationAvoidReply(idNumber), null));
        }

        // Keyword-only intents: skipped when the message is off-topic ("tell me a
        // joke about medicine") or already answered by a more specific part.
        if (!offTopic) {
            if (!medicationAvoid && !foodMatcher.find(0)
                    && (containsAny(lower, "diet", "food guideline") || (matched.isEmpty() && containsAny(lower, "eat", "avoid")))) {
                parts.add(new Part(buildDietReply(idNumber), null));
            }
            boolean asksForOwnMedicines = Pattern.compile(
                    "\\b(?:my|am i on|i'm on|im on|i take|i am taking)\\b.{0,15}\\b(?:medications?|medicines?|meds|pills|prescriptions?|tablets)\\b"
                    + "|\\bwhat (?:medications?|medicines?|meds|pills|tablets)\\b").matcher(lower).find();
            if (!interaction && medicineSafety == null && !medicationAvoid
                    && (asksForOwnMedicines || (matched.isEmpty() && containsAny(lower, "medication", "medicine", "prescription", "pills", "dose")))) {
                parts.add(new Part(buildMedicationReply(patient), null));
            }
            if (!medicationAvoid && containsAny(lower, "allerg")) {
                parts.add(new Part(buildAllergyReply(idNumber), null));
            }
        }

        if (parts.isEmpty()) {
            return new ChatbotResponse(offTopic ? SCOPE_REPLY : FALLBACK_REPLY, null, null);
        }

        String reply = parts.stream().map(Part::text).collect(Collectors.joining("\n\n"));
        if (offTopic) {
            reply += "\n\n(" + SCOPE_REPLY + ")";
        }
        String urgency = parts.stream().map(Part::urgency).filter(u -> u != null)
                .max(Comparator.comparingInt(URGENCY_ORDER::indexOf)).orElse(null);
        return new ChatbotResponse(reply, urgency, symptomNames);
    }

    private Set<Symptom> matchSymptoms(String lower) {
        Set<Symptom> matched = new LinkedHashSet<>();
        List<Token> tokens = tokens(lower);
        for (Symptom symptom : symptomRepository.findAll()) {
            boolean hit = mentioned(lower, tokens, symptom.getName().toLowerCase());
            if (!hit) {
                for (String synonym : SYNONYMS.getOrDefault(symptom.getName(), List.of())) {
                    if (mentioned(lower, tokens, synonym)) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) {
                matched.add(symptom);
            }
        }
        return matched;
    }

    private Part symptomPart(Patient patient, String idNumber, String lower, Set<Symptom> matched) {
        List<Long> symptomIds = matched.stream().map(Symptom::getId).collect(Collectors.toList());
        SymptomCheck check = symptomCheckerService.performSymptomCheck(idNumber, symptomIds);
        String urgency = check.getUrgencyLevel();
        String recommendation = check.getRecommendation().split("\\[")[0].trim();

        List<SymptomAdvice.Guide> guides = new ArrayList<>();
        for (Symptom symptom : matched) {
            symptomAdvice.forSymptom(symptom.getName())
                    .filter(guide -> !guides.contains(guide))
                    .ifPresent(guides::add);
        }

        StringBuilder sb = new StringBuilder("I understood: ")
                .append(matched.stream().map(s -> s.getName().toLowerCase()).collect(Collectors.joining(", ")))
                .append(".\n\n");

        // A danger sign for any of the symptoms overrides the home-care advice.
        for (SymptomAdvice.Guide guide : guides) {
            if (!guide.dangerSigns().isEmpty() && containsAny(lower, guide.dangerSigns().toArray(new String[0]))) {
                sb.append("⚠️ ").append(guide.dangerReason())
                        .append(" Call 10177 for an ambulance (112 from a cell phone) or go to the nearest emergency unit now.");
                return new Part(sb.toString(), "RED");
            }
        }

        if (guides.isEmpty()) {
            sb.append(recommendation);
        } else {
            // The triage headline only ("🟢 Home care recommended"): the guides carry the detail.
            int colon = recommendation.indexOf(':');
            sb.append(colon > 0 ? recommendation.substring(0, colon) + "." : recommendation);
        }
        if (SEVERE.matcher(lower).find() && "GREEN".equals(urgency)) {
            urgency = "YELLOW";
            sb.append("\nYou said it's bad. If it doesn't ease within a few hours of rest, go to the clinic today.");
        }

        boolean paracetamolAllergy = !allergyCheckService.findConflicts("paracetamol",
                allergyRepository.findByPatientId(patient.getId())).isEmpty();
        for (SymptomAdvice.Guide guide : guides.subList(0, Math.min(MAX_GUIDES, guides.size()))) {
            sb.append("\n\n").append(guide.title()).append(": what you can do");
            for (String step : guide.selfCare()) {
                sb.append("\n• ").append(step);
            }
            if (guide.painRelief()) {
                sb.append(paracetamolAllergy
                        ? "\n• Your record shows an allergy that may include paracetamol, so don't take it. Ask your pharmacist what is safe for you."
                        : "\n• Paracetamol can help: follow the dose on the pack, and no more than 4 doses in 24 hours.");
            }
            sb.append("\nGo to the clinic if ").append(guide.seeClinicIf());
        }
        return new Part(sb.toString(), urgency);
    }

    private Part medicineSafetyPart(Patient patient, String lower) {
        String text;
        if (MISSED_DOSE.matcher(lower).find()) {
            text = "If you miss a dose, take it as soon as you remember, unless it's almost time for the next one: then skip the "
                    + "missed dose and carry on as normal. Never take two doses at once to catch up. Your pharmacist can tell you "
                    + "the exact rule for your medicine.";
        } else if (STOP_MEDICINE.matcher(lower).find()) {
            text = "Please don't stop or change your medicine on your own. Some medicines (ARVs, TB treatment, insulin, blood "
                    + "pressure and epilepsy medicine, antibiotics) can become dangerous or stop working if stopped suddenly. "
                    + "If it's causing side effects or isn't helping, tell your clinic: they can change it safely.";
        } else if (MORE_MEDICINE.matcher(lower).find() && (containsAny(lower, "double") || ASKING.matcher(lower).find())) {
            text = "Don't take more than your prescribed dose, or double up: too much can cause serious harm (too much paracetamol, "
                    + "for example, damages the liver). If your dose isn't helping, speak to your clinic or pharmacist.";
        } else if (ANTIBIOTIC_REQUEST.matcher(lower).find()) {
            text = "Only a nurse or doctor can decide whether you need antibiotics, after examining you. They don't help colds or "
                    + "flu (those are viruses), and taking them when not needed makes them work less well when you do need them.";
        } else if (SOMEONE_ELSES_MEDICINE.matcher(lower).find()) {
            text = "Please don't take medicine prescribed for someone else. It may be the wrong medicine or dose for you, or clash "
                    + "with your own medicine or allergies. Visit the clinic and they can give you your own.";
        } else if (OTHER_PEOPLES_RECORDS.matcher(lower).find()) {
            text = "I can only show your own record. Other people's health information is private: they can ask the clinic for "
                    + "theirs themselves.";
        } else {
            return null;
        }
        List<Prescription> active = prescriptionRepository.findByPatientIdAndActiveTrue(patient.getId());
        if (!active.isEmpty() && !OTHER_PEOPLES_RECORDS.matcher(lower).find()) {
            text += "\n\nYour current medicines: " + active.stream().map(Prescription::getMedication).collect(Collectors.joining(", ")) + ".";
        }
        return new Part(text, null);
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

    private record Token(String word, int start) {}

    private static List<Token> tokens(String text) {
        List<Token> tokens = new ArrayList<>();
        Matcher m = WORD.matcher(text);
        while (m.find()) {
            tokens.add(new Token(m.group(), m.start()));
        }
        return tokens;
    }

    // True if the term appears (as words, allowing a small typo in longer
    // words: "headach", "diarhea") and at least once isn't negated in the same
    // clause just before it ("no fever", "I don't have a headache").
    private boolean mentioned(String text, List<Token> tokens, String term) {
        Matcher m = Pattern.compile("\\b" + Pattern.quote(term)).matcher(text);
        while (m.find()) {
            if (!negatedBefore(text, m.start())) {
                return true;
            }
        }
        String[] wanted = term.split("\\s+");
        for (int i = 0; i + wanted.length <= tokens.size(); i++) {
            boolean all = true;
            for (int j = 0; j < wanted.length && all; j++) {
                all = closeEnough(tokens.get(i + j).word(), wanted[j]);
            }
            if (all && !negatedBefore(text, tokens.get(i).start())) {
                return true;
            }
        }
        return false;
    }

    // Real words one letter away from a symptom word ("it's chilly" is not chills).
    private static final Set<String> NOT_TYPOS = Set.of("chilly", "blocker", "header", "breeding", "selling", "spelling",
            "waiting", "washing", "peeing", "dying", "weigh", "infection", "infections", "connection", "increases", "contact");

    // One wrong, missing or extra letter is forgiven, only in words of 6+
    // letters with the same first letter: shorter words are too close to
    // ordinary ones (rash/rush, cough/couch).
    private static boolean closeEnough(String word, String target) {
        if (word.equals(target)) {
            return true;
        }
        if (target.length() < 6 || word.isEmpty() || word.charAt(0) != target.charAt(0)
                || Math.abs(word.length() - target.length()) > 1 || NOT_TYPOS.contains(word)) {
            return false;
        }
        return editDistance(word, target) <= 1;
    }

    private static int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    private boolean negatedBefore(String text, int start) {
        String before = text.substring(Math.max(0, start - 30), start);
        int clauseStart = Math.max(
                Math.max(before.lastIndexOf('.'), before.lastIndexOf(',')),
                Math.max(Math.max(before.lastIndexOf(';'), before.lastIndexOf('!')),
                        Math.max(before.lastIndexOf('?'), before.lastIndexOf(" but "))));
        String clause = clauseStart >= 0 ? before.substring(clauseStart + 1) : before;
        return NEGATION.matcher(clause).find();
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
