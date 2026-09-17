package com.udhr.config;

import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
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
    private VisitRepository visitRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private com.udhr.service.ImmunizationService immunizationService;

    @Autowired
    private ImmunizationRepository immunizationRepository;

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
            admin.setContactNumber("0731234567");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setActive(true);
            admin.setMustChangePassword(false); // documented demo login, not a real temp password
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
            doctor.setMustChangePassword(false); // documented demo login, not a real temp password
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
            nurse.setMustChangePassword(false); // documented demo login, not a real temp password
            staffRepository.save(nurse);

            // Staff 4: PHARM001
            Staff pharmacist = new Staff();
            pharmacist.setStaffNumber("PHARM001");
            pharmacist.setFirstName("Sipho");
            pharmacist.setLastName("Nkosi");
            pharmacist.setRole("PHARMACIST");
            pharmacist.setFacility(facility);
            pharmacist.setEmail("sipho.nkosi@udhr.gov.za");
            pharmacist.setPassword(passwordEncoder.encode("Pharmacy@123"));
            pharmacist.setActive(true);
            pharmacist.setMustChangePassword(false); // documented demo login, not a real temp password
            staffRepository.save(pharmacist);

            System.out.println("Facility and Staff seeded successfully.");
        } else {
            facility = facilityRepository.findAll().get(0);
        }

        // 2. Seed Test Patient (Reginal Themba, with Diabetes, Hypertension, Penicillin Allergy)
        Patient patient = null;
        if (patientRepository.count() == 0) {
            patient = new Patient();
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

        // 3. Seed a mother + newborn baby to demonstrate a file opened from
        // birth, before any national ID number exists. The baby is found by
        // UHID, not idNumber, and is linked back to the mother's record.
        if (patientRepository.count() <= 1 && facility != null) {
            Patient mother = new Patient();
            mother.setIdNumber("8805120123089");
            mother.setFirstName("Nomvula");
            mother.setLastName("Dlamini");
            mother.setDateOfBirth(LocalDate.of(1988, 5, 12));
            mother.setGender("FEMALE");
            mother.setContactNumber("0827654321");
            mother.setEmail("nomvula@udhr.gov.za");
            mother.setAddress("45 Kruger Street, Nelspruit, Mpumalanga");
            mother = patientRepository.save(mother);

            Patient baby = new Patient();
            // No ID number: the birth has not yet been registered with Home
            // Affairs. The baby's file is still fully usable via its UHID.
            baby.setFirstName("Baby");
            baby.setLastName("Dlamini");
            baby.setDateOfBirth(LocalDate.now().minusWeeks(2));
            baby.setGender("FEMALE");
            baby.setAddress(mother.getAddress());
            baby.setMotherPatient(mother);
            baby.setBirthFacility(facility);
            baby.setBirthWeightGrams(3200);
            baby.setBirthLengthCm(new java.math.BigDecimal("49.5"));
            baby.setApgarScore1Min(9);
            baby.setApgarScore5Min(10);
            baby = patientRepository.save(baby);

            List<Immunization> schedule = immunizationService.generateEpiSchedule(baby);
            // Mark the two birth-dose vaccines as already given
            for (Immunization dose : schedule) {
                if (dose.getScheduledDate().equals(baby.getDateOfBirth())) {
                    dose.setStatus("GIVEN");
                    dose.setAdministeredDate(baby.getDateOfBirth());
                    dose.setFacility(facility);
                    dose.setNotes("Administered at birth.");
                    immunizationRepository.save(dose);
                }
            }

            System.out.println("Seeded mother 'Nomvula Dlamini' and newborn baby (UHID: " + baby.getUhid() + ", no ID number yet) with EPI schedule.");
        }

        // 4. Seed Visits and Diagnoses with official ICD-10 codes
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

}
