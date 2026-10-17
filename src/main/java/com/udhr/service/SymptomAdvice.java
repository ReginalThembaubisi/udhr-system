package com.udhr.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a patient can do about a common symptom, written for the patient: a
 * few things to try at home, when to go to the clinic, and the danger signs
 * that mean emergency care now.
 *
 * Curated, general primary-care advice in the spirit of the South African
 * clinic guidelines, not a diagnosis. Medicines are limited to paracetamol "as
 * on the pack", and ChatbotService drops even that for a patient whose record
 * shows a paracetamol allergy. Each guide is keyed by the seeded Symptom name,
 * and several names can share one guide (Mild Fever, High Fever).
 */
@Component
public class SymptomAdvice {

    /**
     * @param dangerSigns words in the patient's message that turn this into an
     *                    emergency (each matched as the start of a word)
     */
    public record Guide(String title, List<String> selfCare, boolean painRelief, String seeClinicIf,
                        List<String> dangerSigns, String dangerReason) {}

    private static final Guide HEADACHE = new Guide("Headache",
            List.of("Drink water: a headache is often from not drinking enough.",
                    "Rest in a quiet, dark room and take a break from screens.",
                    "Eat something if you haven't eaten for a while."),
            true,
            "it lasts more than 3 days, keeps coming back, or wakes you up at night.",
            List.of("worst", "sudden", "stiff neck", "hit my head", "head injury", "after a fall", "fell on my head",
                    "confus", "can't see", "cant see", "blurred vision", "weak on one side", "slurred", "fainted"),
            "A sudden or very bad headache, or one with a stiff neck, confusion, a head injury or vision changes, can be serious.");

    private static final Guide FEVER = new Guide("Fever",
            List.of("Drink plenty of fluids, little and often.",
                    "Wear light clothes and keep the room cool; don't wrap up in blankets.",
                    "Rest."),
            true,
            "it lasts more than 2 days, is very high, or you also have a cough, pain passing urine or a rash.",
            List.of("stiff neck", "had a fit", "seizure", "confus", "rash that", "purple", "can't wake", "cant wake", "floppy"),
            "A fever with a stiff neck, confusion, a fit or a rash that doesn't fade under a glass can be serious.");

    private static final Guide COUGH = new Guide("Cough",
            List.of("Drink warm fluids; warm water with honey and lemon can soothe it (no honey for babies under 1).",
                    "Rest, and avoid smoke and smoking.",
                    "Cover your mouth when you cough and wash your hands."),
            false,
            "it lasts more than 2 weeks (ask for a TB test), you cough up blood, lose weight or sweat at night.",
            List.of("coughing blood", "cough up blood", "coughing up blood", "blood in my", "blue lips", "can't talk", "cant talk"),
            "Coughing up blood or blue lips needs urgent care.");

    private static final Guide SORE_THROAT = new Guide("Sore throat",
            List.of("Gargle with warm salt water a few times a day.",
                    "Drink warm fluids and suck on throat lozenges.",
                    "Rest your voice."),
            true,
            "it lasts more than a week, you have a high fever, white spots on the tonsils, or it hurts to swallow.",
            List.of("can't swallow", "cant swallow", "drool", "can't open my mouth"),
            "Not being able to swallow or open your mouth can mean a serious throat infection.");

    private static final Guide COLD = new Guide("Cold or flu",
            List.of("Rest and drink plenty of fluids.",
                    "Steam from a bowl of hot water can ease a blocked nose (take care not to burn yourself).",
                    "Antibiotics don't help colds or flu: they are caused by viruses."),
            true,
            "you're not better after 10 days, or you're pregnant, older than 65, or have diabetes, asthma or HIV.",
            List.of(),
            null);

    private static final Guide DIARRHOEA = new Guide("Diarrhoea",
            List.of("Drink a lot: oral rehydration solution (ORS) after every loose stool.",
                    "Make ORS at home: 1 litre of clean water, 8 level teaspoons of sugar and half a level teaspoon of salt.",
                    "Keep eating small, light meals, and wash your hands well."),
            false,
            "it lasts more than 2 days, or you also have a fever or stomach pain.",
            List.of("blood in", "bloody", "black stool", "no urine", "not urinat", "not peeing", "can't keep", "cant keep", "very weak"),
            "Blood in the stool, not passing urine or not keeping fluids down can mean dangerous dehydration.");

    private static final Guide VOMITING = new Guide("Vomiting or nausea",
            List.of("Take small sips of water or ORS every few minutes rather than big drinks.",
                    "When it settles, eat small amounts of plain food (bread, rice, porridge).",
                    "Rest."),
            false,
            "it lasts more than a day, or you can't keep any fluids down.",
            List.of("vomiting blood", "blood in", "green vomit", "vomiting green", "black vomit", "coffee grounds", "head injury",
                    "hit my head", "stiff neck", "can't keep", "cant keep"),
            "Vomiting blood or green fluid, or vomiting after a head injury, needs urgent care.");

    private static final Guide STOMACH = new Guide("Stomach pain",
            List.of("Rest, and try a warm (not hot) water bottle on your tummy.",
                    "Eat small, plain meals and avoid fatty or spicy food and alcohol.",
                    "Drink water."),
            false,
            "it lasts more than a day, keeps coming back, or you also have a fever, vomiting or diarrhoea.",
            List.of("severe", "unbearable", "pregnan", "blood in", "vomiting blood", "hard tummy", "hard stomach", "can't move", "cant move"),
            "Severe stomach pain, pain in pregnancy or pain with blood can be serious.");

    private static final Guide RASH = new Guide("Rash or itching",
            List.of("Keep the skin clean and dry, and don't scratch.",
                    "Wear loose cotton clothes.",
                    "Think about anything new: soap, washing powder, food or medicine."),
            false,
            "it spreads, blisters, is painful, or you also have a fever. If it started after a new medicine, tell the clinic.",
            List.of("swollen face", "swollen lips", "swollen tongue", "lips swell", "face swell", "purple", "peeling"),
            "A rash with a swollen face or lips can be a serious allergic reaction.");

    private static final Guide BACK_PAIN = new Guide("Back pain",
            List.of("Keep gently moving; long bed rest makes it slower to heal.",
                    "A warm compress can ease the muscles.",
                    "Bend your knees when you lift, not your back."),
            true,
            "it lasts more than 2 weeks, or started after a fall or injury.",
            List.of("numb", "can't control", "cant control", "bladder", "can't walk", "cant walk", "weak legs"),
            "Back pain with numbness, weak legs or losing control of your bladder needs urgent care.");

    private static final Guide TIREDNESS = new Guide("Tiredness",
            List.of("Aim for 7 to 9 hours of sleep and a regular bedtime.",
                    "Eat regular meals and drink water.",
                    "A short walk each day can help your energy."),
            false,
            "it lasts more than 2 weeks, or you also lose weight, sweat at night, feel very thirsty or pass a lot of urine.",
            List.of(),
            null);

    private static final Guide DIZZINESS = new Guide("Dizziness",
            List.of("Sit or lie down until it passes, then stand up slowly.",
                    "Drink water.",
                    "Don't drive or climb while you feel dizzy."),
            false,
            "it keeps happening, or you take medicine for blood pressure or diabetes.",
            List.of("fainted", "chest", "weak on one side", "slurred", "can't walk", "cant walk"),
            "Dizziness with fainting, chest pain, slurred speech or weakness on one side can be serious.");

    private static final Map<String, Guide> BY_SYMPTOM = new LinkedHashMap<>();

    static {
        BY_SYMPTOM.put("Headache", HEADACHE);
        BY_SYMPTOM.put("High Fever", FEVER);
        BY_SYMPTOM.put("Mild Fever", FEVER);
        BY_SYMPTOM.put("Cough", COUGH);
        BY_SYMPTOM.put("Throat Irritation", SORE_THROAT);
        BY_SYMPTOM.put("Runny Nose", COLD);
        BY_SYMPTOM.put("Congestion", COLD);
        BY_SYMPTOM.put("Diarrhoea", DIARRHOEA);
        BY_SYMPTOM.put("Vomiting", VOMITING);
        BY_SYMPTOM.put("Nausea", VOMITING);
        BY_SYMPTOM.put("Stomach Pain", STOMACH);
        BY_SYMPTOM.put("Abdominal Pain", STOMACH);
        BY_SYMPTOM.put("Belly Pain", STOMACH);
        BY_SYMPTOM.put("Skin Rash", RASH);
        BY_SYMPTOM.put("Itching", RASH);
        BY_SYMPTOM.put("Back Pain", BACK_PAIN);
        BY_SYMPTOM.put("Fatigue", TIREDNESS);
        BY_SYMPTOM.put("Lethargy", TIREDNESS);
        BY_SYMPTOM.put("Dizziness", DIZZINESS);
    }

    public Optional<Guide> forSymptom(String symptomName) {
        return Optional.ofNullable(BY_SYMPTOM.get(symptomName));
    }
}
