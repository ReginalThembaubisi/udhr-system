package com.udhr.config;

import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private ChronicConditionRepository chronicConditionRepository;

    @Autowired
    private SymptomRepository symptomRepository;

    @Autowired
    private HealthTipRepository healthTipRepository;

    @Autowired
    private DietaryGuidelineRepository dietaryGuidelineRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private IngredientWarningRepository ingredientWarningRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Facility & Staff
        Facility facility = null;
        if (facilityRepository.count() == 0) {
            facility = new Facility();
            facility.setName("Rob Ferreira Hospital");
            facility.setType("HOSPITAL");
            facility.setProvince("Mpumalanga");
            facility.setAddress("Nelspruit, Mpumalanga");
            facility = facilityRepository.save(facility);

            // Staff 1: ADMIN001
            Staff admin = new Staff();
            admin.setStaffNumber("ADMIN001");
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setRole("ADMIN");
            admin.setFacility(facility);
            admin.setEmail("admin@udhr.gov.za");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setActive(true);
            staffRepository.save(admin);

            // Staff 2: DOC001
            Staff doctor = new Staff();
            doctor.setStaffNumber("DOC001");
            doctor.setFirstName("Themba");
            doctor.setLastName("Ubisi");
            doctor.setRole("DOCTOR");
            doctor.setFacility(facility);
            doctor.setEmail("themba@udhr.gov.za");
            doctor.setPassword(passwordEncoder.encode("Doctor@123"));
            doctor.setActive(true);
            staffRepository.save(doctor);

            // Staff 3: NUR001
            Staff nurse = new Staff();
            nurse.setStaffNumber("NUR001");
            nurse.setFirstName("Zanele");
            nurse.setLastName("Mokoena");
            nurse.setRole("NURSE");
            nurse.setFacility(facility);
            nurse.setEmail("zanele@udhr.gov.za");
            nurse.setPassword(passwordEncoder.encode("Nurse@123"));
            nurse.setActive(true);
            staffRepository.save(nurse);

            // Staff 4: PHARM001
            Staff pharmacist = new Staff();
            pharmacist.setStaffNumber("PHARM001");
            pharmacist.setFirstName("Sipho");
            pharmacist.setLastName("Nkosi");
            pharmacist.setRole("PHARMACIST");
            pharmacist.setFacility(facility);
            pharmacist.setEmail("sipho@udhr.gov.za");
            pharmacist.setPassword(passwordEncoder.encode("Pharmacist@123"));
            pharmacist.setActive(true);
            staffRepository.save(pharmacist);

            System.out.println("Facility and Staff seeded successfully.");
        } else {
            facility = facilityRepository.findAll().get(0);
        }

        // 2. Seed Master Guidelines & Tips (South African context)
        seedGuidelinesAndTips();

        // 3. Seed Kaggle Disease-Symptom Dataset Mappings
        seedKaggleDiseasesAndSymptoms();

        // 4. Seed Ingredients and Warnings
        if (ingredientRepository.count() == 0) {
            Ingredient sugar = createIngredient("Sugar", "Sweetener", "Simple carbohydrate that elevates blood glucose.");
            Ingredient sodium = createIngredient("Sodium", "Mineral", "Electrolyte that affects fluid balance and blood pressure.");
            Ingredient peanuts = createIngredient("Peanuts", "Allergen", "Highly allergenic legume seed.");
            Ingredient transFat = createIngredient("Trans Fat", "Fat", "Hydrogenated vegetable oil with negative cardiovascular profile.");
            Ingredient potassium = createIngredient("Potassium", "Mineral", "Helpful electrolyte that counters sodium effects.");
            Ingredient fiber = createIngredient("Fiber", "Carbohydrate", "Indigestible plant matter that regulates glucose absorption.");
            Ingredient wheat = createIngredient("Wheat Flour", "Grain", "Gluten-containing staple grain flour.");

            createIngredientWarning(sugar, "Diabetes", "DANGER", "Refined sugar elevates blood glucose rapidly, worsening glycemic control.");
            createIngredientWarning(sodium, "Hypertension", "DANGER", "Excess sodium intake retains fluids and raises systemic arterial pressure.");
            createIngredientWarning(peanuts, "Peanuts", "DANGER", "Triggers severe hypersensitivity or anaphylaxis in allergic patients.");
            createIngredientWarning(peanuts, "Nuts", "DANGER", "Cross-reactivity warning. Avoid to prevent allergic reactions.");
            createIngredientWarning(transFat, "Hypertension", "CAUTION", "Promotes arterial plaque buildup, increasing hypertension stroke risks.");
            createIngredientWarning(potassium, "Hypertension", "SAFE", "Potassium aids in vascular relaxation and helps lower blood pressure.");
            createIngredientWarning(fiber, "Diabetes", "SAFE", "Dietary fiber delays sugar absorption and prevents glucose spikes.");
            createIngredientWarning(wheat, "Wheat", "DANGER", "Triggers celiac response or wheat allergy symptoms.");
            System.out.println("Ingredients and Warnings seeded successfully.");
        }

        // 5. Seed Test Patient (Reginal Themba, with Diabetes, Hypertension, Penicillin Allergy)
        Patient patient = null;
        if (patientRepository.count() == 0) {
            patient = new Patient();
            patient.setMrn("MRN-2020-000001");
            patient.setIdNumber("9001015000083");
            patient.setFirstName("Reginal");
            patient.setLastName("Themba");
            patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
            patient.setGender("MALE");
            patient.setContactNumber("0821234567");
            patient.setEmail("reginal@udhr.gov.za");
            patient.setAddress("123 Mandela Drive, Nelspruit, Mpumalanga");
            patient = patientRepository.save(patient);

            // Add Chronic Conditions
            createChronicCondition(patient, "Diabetes", LocalDate.of(2020, 5, 15), "Type 2 diabetes mellitus, managed with Metformin.");
            createChronicCondition(patient, "Hypertension", LocalDate.of(2021, 8, 20), "Primary hypertension, managed with Amlodipine.");

            // Add Allergy
            createAllergy(patient, "Penicillin", "SEVERE", "Anaphylaxis and hives upon exposure to penicillin antibiotics.");

            System.out.println("Test patient 'Reginal Themba' (ID: 9001015000083) seeded successfully.");
        } else {
            patient = patientRepository.findAll().get(0);
        }

        // 6. Seed Visits and Diagnoses with official ICD-10 codes
        if (visitRepository.count() == 0 && diagnosisRepository.count() == 0 && patient != null) {
            Staff doctor = staffRepository.findByStaffNumber("DOC001").orElse(null);
            if (doctor != null && facility != null) {
                // Visit 1: Diabetes Checkup
                Visit visit1 = new Visit();
                visit1.setPatient(patient);
                visit1.setStaff(doctor);
                visit1.setFacility(facility);
                visit1.setReason("Routine chronic disease management checkup for blood sugar levels.");
                visit1.setNotes("Patient reports mild fatigue but overall stable.");
                visit1 = visitRepository.save(visit1);

                Diagnosis diag1 = new Diagnosis();
                diag1.setPatient(patient);
                diag1.setVisit(visit1);
                diag1.setDoctor(doctor);
                diag1.setFacility(facility);
                diag1.setDiagnosis("Diabetes Mellitus Type 2");
                diag1.setIcd10Code("E11");
                diag1.setNotes("Managed with Metformin 500mg BD. HbA1c is 6.8%. Complies with dietary advice.");
                diagnosisRepository.save(diag1);

                // Visit 2: Hypertension Checkup
                Visit visit2 = new Visit();
                visit2.setPatient(patient);
                visit2.setStaff(doctor);
                visit2.setFacility(facility);
                visit2.setReason("Hypertension monitoring and medication refill.");
                visit2.setNotes("BP measured at 135/85 mmHg.");
                visit2 = visitRepository.save(visit2);

                Diagnosis diag2 = new Diagnosis();
                diag2.setPatient(patient);
                diag2.setVisit(visit2);
                diag2.setDoctor(doctor);
                diag2.setFacility(facility);
                diag2.setDiagnosis("Essential Hypertension");
                diag2.setIcd10Code("I10");
                diag2.setNotes("Managed with Amlodipine 5mg daily. BP stable, encouraged low salt diet.");
                diagnosisRepository.save(diag2);
                
                // Seed Prescriptions for Reginal Themba
                if (prescriptionRepository.count() == 0) {
                    Prescription p1 = new Prescription();
                    p1.setPatient(patient);
                    p1.setDoctor(doctor);
                    p1.setFacility(facility);
                    p1.setVisit(visit1);
                    p1.setMedication("Metformin 500mg");
                    p1.setDosage("1 Tablet");
                    p1.setFrequency("Twice daily");
                    p1.setStartDate(LocalDate.now());
                    p1.setEndDate(LocalDate.now().plusDays(30));
                    p1.setActive(true);
                    p1.setNotes("Take with meals.");
                    p1 = prescriptionRepository.save(p1);
                    reminderService.createRemindersForPrescription(p1);

                    Prescription p2 = new Prescription();
                    p2.setPatient(patient);
                    p2.setDoctor(doctor);
                    p2.setFacility(facility);
                    p2.setVisit(visit2);
                    p2.setMedication("Amlodipine 5mg");
                    p2.setDosage("1 Tablet");
                    p2.setFrequency("Once daily");
                    p2.setStartDate(LocalDate.now());
                    p2.setEndDate(LocalDate.now().plusDays(30));
                    p2.setActive(true);
                    p2.setNotes("Take in the morning.");
                    p2 = prescriptionRepository.save(p2);
                    reminderService.createRemindersForPrescription(p2);

                    System.out.println("Prescriptions and medication reminders seeded successfully.");
                }
                
                System.out.println("Visits and Diagnoses with ICD-10 codes seeded successfully.");
            }
        }
    }

    private void seedGuidelinesAndTips() {
        if (dietaryGuidelineRepository.count() == 0) {
            // Local SA Diabetic guidelines (Diabetic South Africa)
            createDietaryGuideline("Diabetes", "AVOID", "Refined White Pap (Maize Meal)", 
                    "Pap has a high glycemic index, causing rapid and severe blood glucose spikes. Replace with brown/whole-wheat porridges.", "Diabetic South Africa");
            createDietaryGuideline("Diabetes", "AVOID", "White Bread (Sasani / Govt Loaf)", 
                    "Refined white flour converts to sugar almost immediately in the body. Choose seeded brown bread instead.", "Diabetic South Africa");
            createDietaryGuideline("Diabetes", "AVOID", "Sugary Mageu and Fizzy Drinks", 
                    "Commercial Mageu and sweet sodas contain high amounts of added sugars that stress insulin production.", "Diabetic South Africa");
            createDietaryGuideline("Diabetes", "EAT", "Morogo / Wild Spinach / Pumpkin Leaves", 
                    "Morogo and other green leafy vegetables are highly recommended. They are low-carb, high-fiber, and improve insulin sensitivity.", "Diabetic South Africa");
            createDietaryGuideline("Diabetes", "EAT", "Whole Seeded Brown Bread", 
                    "High-fiber whole wheat bread releases carbohydrates slowly, maintaining stable blood sugar.", "Diabetic South Africa");
            createDietaryGuideline("Diabetes", "EAT", "Pure Clean Water", 
                    "Stay hydrated with clean water instead of sugary beverages to help kidneys flush excess glucose.", "Diabetic South Africa");

            // Hypertension (Heart & Stroke Foundation SA)
            createDietaryGuideline("Hypertension", "AVOID", "Salty Crisps / Potato Chips", 
                    "Potato chips contain high concentrations of sodium chloride, causing acute water retention and raising blood pressure.", "Heart & Stroke Foundation SA");
            createDietaryGuideline("Hypertension", "AVOID", "Processed Meats (Polony, Boerewors, Biltong)", 
                    "Cured South African favorites polony, boerewors, and biltong are heavily loaded with salt used for curing.", "Heart & Stroke Foundation SA");
            createDietaryGuideline("Hypertension", "AVOID", "Fast Foods and Takeaways", 
                    "Takeaway fried chicken, commercial burgers, and chips contain extreme levels of sodium and trans fats.", "Heart & Stroke Foundation SA");
            createDietaryGuideline("Hypertension", "EAT", "Unsalted Beans and Legumes", 
                    "Beans, lentils, and peas are high in fiber, potassium, and magnesium, which help dilate blood vessels.", "Heart & Stroke Foundation SA");

            createHealthTip("Hypertension", "LIFESTYLE", "Limit Salt to Under 5g Daily", 
                    "Keep daily salt intake to under 5g (approximately one level teaspoon). Avoid adding table salt to food.", "Heart & Stroke Foundation SA");
            createHealthTip("Hypertension", "LIFESTYLE", "150 Minutes Weekly Exercise Plan", 
                    "Perform at least 150 minutes of moderate exercise (like brisk walking or gardening) per week to strengthen the heart.", "Heart & Stroke Foundation SA");

            // TB (South African Department of Health Guidelines)
            createHealthTip("Tuberculosis", "MEDICATION", "Complete Full 6-Month TB Regimen", 
                    "Strictly adhere to the 6-month treatment course (Rifafour/Rifampicin, Isoniazid, etc.) to prevent Multidrug-Resistant TB (MDR-TB).", "South African Department of Health");
            createHealthTip("Tuberculosis", "GENERAL", "Adequate Natural Ventilation", 
                    "Keep windows and doors open in public transport (minibus taxis) and homes to ensure air dilution and reduce TB transmission.", "South African Department of Health");
            createDietaryGuideline("Tuberculosis", "EAT", "High Protein (Beans, Lentils, Eggs, Lean Meat)", 
                    "TB causes severe muscle wasting. High-protein foods are critical to rebuilding body mass and supporting immune recovery.", "South African Department of Health");
            createDietaryGuideline("Tuberculosis", "AVOID", "Alcohol and Tobacco", 
                    "TB drugs are processed by the liver; alcohol raises the risk of drug-induced hepatitis. Smoking damages the lungs further.", "South African Department of Health");

            // HIV / ARVs (South African Department of Health Guidelines)
            createHealthTip("HIV/AIDS", "MEDICATION", "Take ARVs at the Same Time Daily", 
                    "Adherence of 95%+ is necessary to suppress the HIV viral load to undetectable levels and prevent drug resistance.", "South African Department of Health");
            createHealthTip("HIV/AIDS", "GENERAL", "Regular CD4 and Viral Load Monitoring", 
                    "Check your viral load annually at your local clinic to ensure that your ARV regimen is successfully suppressing the virus.", "South African Department of Health");
            createDietaryGuideline("HIV/AIDS", "EAT", "Fully Cooked Warm Meals", 
                    "Prevent opportunistic foodborne gastrointestinal infections by eating only freshly cooked, warm foods.", "South African Department of Health");
            createDietaryGuideline("HIV/AIDS", "AVOID", "Unpasteurized Dairy and Raw Eggs", 
                    "Immune-compromised individuals are highly susceptible to Listeria and Salmonella. Avoid raw eggs, sushi, and unpasteurized milk.", "South African Department of Health");

            // Malaria (South African Department of Health Guidelines)
            createHealthTip("Malaria", "GENERAL", "Malaria Prevention in Mpumalanga", 
                    "Mpumalanga (especially Ehlanzeni district) is a seasonal malaria risk zone. Sleep under insecticide-treated bed nets and wear long sleeves.", "South African Department of Health");
            createHealthTip("Malaria", "MEDICATION", "Take Prescribed Chemoprophylaxis", 
                    "When travelling to Limpopo or Mpumalanga border areas, take chemoprophylaxis (Mefloquine, Doxycycline) as advised.", "South African Department of Health");
            createDietaryGuideline("Malaria", "EAT", "Hydrating Electrolytes and Fluids", 
                    "Maintain hydration with clean water, herbal teas, or oral rehydration solutions to replace fluids lost due to high fevers.", "South African Department of Health");

            // NICD Disease Alerts
            createHealthTip("Malaria", "GENERAL", "NICD Malaria Risk Alerts", 
                    "Limpopo (Vhembe and Mopani) and Mpumalanga (Ehlanzeni/Kruger Area) are endemic malaria zones. Peak transmission is October to May.", "National Institute for Communicable Diseases (NICD)");
            createHealthTip("General Health", "GENERAL", "NICD Seasonal Influenza Alert", 
                    "South African winter flu season runs May to August. Annual flu vaccination is highly recommended for chronic patients.", "National Institute for Communicable Diseases (NICD)");
            createHealthTip("General Health", "GENERAL", "NICD Cholera Prevention Warning", 
                    "Avoid drinking untreated water. Treat suspect water with household bleach (1 teaspoon per 25 liters) and wash hands regularly.", "National Institute for Communicable Diseases (NICD)");
            
            System.out.println("Guidelines and Health Tips seeded successfully.");
        }
    }

    private void seedKaggleDiseasesAndSymptoms() {
        if (symptomRepository.count() > 20) {
            // Already seeded by CSV
            return;
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("training_data.csv")) {
            if (is == null) {
                System.err.println("Kaggle training_data.csv not found in classpath.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String headerLine = reader.readLine();
                if (headerLine == null) return;

                // Parse symptoms list from header (all columns except the last one 'prognosis')
                String[] symptomsHeader = headerLine.split(",");
                int symptomCount = symptomsHeader.length - 1; // Last column is prognosis

                // Clean and seed the unique symptoms
                Symptom[] symptomsObj = new Symptom[symptomCount];
                for (int i = 0; i < symptomCount; i++) {
                    String rawName = symptomsHeader[i].trim();
                    String cleanName = cleanSymptomName(rawName);

                    // Check if symptom already exists
                    Optional<Symptom> existing = symptomRepository.findByName(cleanName);
                    if (existing.isPresent()) {
                        symptomsObj[i] = existing.get();
                    } else {
                        Symptom s = new Symptom();
                        s.setName(cleanName);
                        s.setInfermedicaId("s_kaggle_" + i);
                        s.setCategory("Kaggle Prediction");
                        s.setIcd10Code("R69"); // General symptom code
                        s.setSource("Kaggle Disease-Symptom Prediction");
                        symptomsObj[i] = symptomRepository.save(s);
                    }
                }

                // Map diseases (prognosis) to symptoms sets
                Map<String, Set<String>> diseaseSymptomsMap = new HashMap<>();
                String row;
                while ((row = reader.readLine()) != null) {
                    String[] values = row.split(",");
                    if (values.length < symptomsHeader.length) continue;

                    String disease = values[symptomCount].trim();
                    diseaseSymptomsMap.putIfAbsent(disease, new HashSet<>());

                    for (int i = 0; i < symptomCount; i++) {
                        if ("1".equals(values[i].trim())) {
                            diseaseSymptomsMap.get(disease).add(symptomsObj[i].getName());
                        }
                    }
                }

                // Seed the disease symptom mappings as health tips of type "SYMPTOMS"
                for (Map.Entry<String, Set<String>> entry : diseaseSymptomsMap.entrySet()) {
                    String disease = entry.getKey();
                    Set<String> diseaseSymptoms = entry.getValue();

                    // Check if health tip already exists
                    boolean exists = healthTipRepository.findAll().stream()
                            .anyMatch(t -> t.getConditionName().equalsIgnoreCase(disease) && "SYMPTOMS".equals(t.getTipType()));

                    if (!exists && !diseaseSymptoms.isEmpty()) {
                        HealthTip tip = new HealthTip();
                        tip.setConditionName(disease);
                        tip.setTipType("SYMPTOMS");
                        tip.setTitle("Common Symptoms for " + disease);
                        tip.setDescription(String.join(", ", diseaseSymptoms));
                        tip.setSource("Kaggle Disease-Symptom Prediction");
                        healthTipRepository.save(tip);
                    }
                }

                System.out.println("Seeded Kaggle symptoms and disease mappings successfully.");
            }
        } catch (Exception e) {
            System.err.println("Failed to seed Kaggle disease-symptom dataset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String cleanSymptomName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String replaced = raw.replace("_", " ").replace(".", " ").trim();
        String[] words = replaced.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)))
              .append(w.substring(1).toLowerCase())
              .append(" ");
        }
        return sb.toString().trim();
    }

    private void createSymptom(String name, String infermedicaId, String category, String icd10Code, String source) {
        Symptom symptom = new Symptom();
        symptom.setName(name);
        symptom.setInfermedicaId(infermedicaId);
        symptom.setCategory(category);
        symptom.setIcd10Code(icd10Code);
        symptom.setSource(source);
        symptomRepository.save(symptom);
    }

    private void createHealthTip(String condition, String type, String title, String description, String source) {
        HealthTip tip = new HealthTip();
        tip.setConditionName(condition);
        tip.setTipType(type);
        tip.setTitle(title);
        tip.setDescription(description);
        tip.setSource(source);
        healthTipRepository.save(tip);
    }

    private void createDietaryGuideline(String condition, String type, String item, String description, String source) {
        DietaryGuideline guideline = new DietaryGuideline();
        guideline.setConditionName(condition);
        guideline.setFoodType(type);
        guideline.setFoodItem(item);
        guideline.setDescription(description);
        guideline.setSource(source);
        dietaryGuidelineRepository.save(guideline);
    }

    private void createChronicCondition(Patient patient, String conditionName, LocalDate diagnosedDate, String notes) {
        ChronicCondition condition = new ChronicCondition();
        condition.setPatient(patient);
        condition.setConditionName(conditionName);
        condition.setDiagnosedDate(diagnosedDate);
        condition.setNotes(notes);
        chronicConditionRepository.save(condition);
    }

    private void createAllergy(Patient patient, String allergen, String severity, String notes) {
        Allergy allergy = new Allergy();
        allergy.setPatient(patient);
        allergy.setAllergen(allergen);
        allergy.setSeverity(severity);
        allergy.setNotes(notes);
        allergyRepository.save(allergy);
    }

    private Ingredient createIngredient(String name, String category, String description) {
        Ingredient ing = new Ingredient();
        ing.setName(name);
        ing.setCategory(category);
        ing.setDescription(description);
        return ingredientRepository.save(ing);
    }

    private void createIngredientWarning(Ingredient ing, String condition, String severity, String reason) {
        IngredientWarning warning = new IngredientWarning();
        warning.setIngredient(ing);
        warning.setConditionName(condition);
        warning.setSeverity(severity);
        warning.setReason(reason);
        ingredientWarningRepository.save(warning);
    }
}
