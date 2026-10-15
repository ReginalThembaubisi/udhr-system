package com.udhr.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Allergy safety from prescribing to dispensing (fix list P0-2, P0-3, P0-4),
 * through the real HTTP layer. The seeded patient 9001015000083 has a SEVERE
 * penicillin allergy (DataSeeder).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AllergySafetyTest {

    private static final String SEEDED_PATIENT_ID = "9001015000083";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    // ---- P0-2: prescribing --------------------------------------------------------

    @Test
    void amoxicillinForPenicillinAllergicPatientIsRefusedWithTheWarning() throws Exception {
        String doctor = login("DOC001", "Doctor@123");
        long before = prescriptionRepository.count();

        MvcResult result = mvc.perform(post("/api/prescriptions").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prescription("Amoxicillin 500mg", null)))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode conflicts = json.readTree(result.getResponse().getContentAsString()).get("allergyConflicts");
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).asText()).contains("penicillin").contains("SEVERE");
        assertThat(prescriptionRepository.count()).isEqualTo(before);
    }

    @Test
    void aVagueOverrideReasonIsNotEnough() throws Exception {
        String doctor = login("DOC001", "Doctor@123");
        mvc.perform(post("/api/prescriptions").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prescription("Amoxicillin 500mg", "ok")))
                .andExpect(status().isConflict());
    }

    @Test
    void anOverrideWithAReasonIsSavedAuditedAndShownToThePharmacist() throws Exception {
        String doctor = login("DOC001", "Doctor@123");
        String reason = "Childhood rash only; tolerated amoxicillin in 2024";
        MvcResult result = mvc.perform(post("/api/prescriptions").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prescription("Augmentin 625mg", reason)))
                .andExpect(status().isCreated())
                .andReturn();
        long prescriptionId = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        Prescription saved = prescriptionRepository.findById(prescriptionId).orElseThrow();
        assertThat(saved.getAllergyOverrideReason()).isEqualTo(reason);
        assertThat(auditLogRepository.findAllByOrderByCreatedAtDesc())
                .anyMatch(log -> "ALLERGY_OVERRIDE".equals(log.getAction()));

        // P0-3: the pharmacist sees the allergy, the warning and the reason.
        String pharmacist = login("PHARM001", "Pharmacist@123");
        JsonNode lookup = json.readTree(mvc.perform(get("/api/pharmacy/patient/" + SEEDED_PATIENT_ID)
                        .header("Authorization", bearer(pharmacist)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(lookup.get("allergies").toString()).contains("Penicillin");
        assertThat(lookup.get("allergyWarnings").get(String.valueOf(prescriptionId))).isNotNull();
        assertThat(lookup.toString()).contains(reason);
    }

    @Test
    void unrelatedMedicineGoesThroughWithoutAReason() throws Exception {
        String doctor = login("DOC001", "Doctor@123");
        mvc.perform(post("/api/prescriptions").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prescription("Paracetamol 500mg", null)))
                .andExpect(status().isCreated());
    }

    // ---- P0-4: recording allergies -------------------------------------------------

    @Test
    void aNewlyRecordedAllergyIsCheckedOnTheNextPrescription() throws Exception {
        String doctor = login("DOC001", "Doctor@123");
        mvc.perform(post("/api/allergies").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "patientId", patientId(), "allergen", "Sulfa",
                                "severity", "severe", "notes", "Stevens-Johnson syndrome"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/prescriptions").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prescription("Co-trimoxazole 960mg", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void allergyNeedsAnAllergenAndAValidSeverity() throws Exception {
        String doctor = login("DOC001", "Doctor@123");
        mvc.perform(post("/api/allergies").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("patientId", patientId(), "allergen", "", "severity", "SEVERE"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/allergies").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("patientId", patientId(), "allergen", "Latex", "severity", "terrible"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyDoctorsAndNursesCanUseTheAllergyApi() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        mvc.perform(get("/api/allergies/patient/" + patientId()).header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/allergies").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("patientId", patientId(), "allergen", "Latex", "severity", "MILD"))))
                .andExpect(status().isForbidden());

        String nurse = login("NUR001", "Nurse@123");
        mvc.perform(get("/api/allergies/patient/" + patientId()).header("Authorization", bearer(nurse)))
                .andExpect(status().isOk());
    }

    // ---- helpers ------------------------------------------------------------------

    private Long patientId() {
        Patient patient = patientRepository.findByIdNumber(SEEDED_PATIENT_ID).orElseThrow();
        return patient.getId();
    }

    private String prescription(String medication, String overrideReason) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("patientId", patientId());
        body.put("medicationName", medication);
        body.put("dosage", "1 tablet");
        body.put("frequency", "Twice daily");
        body.put("durationDays", 5);
        body.put("dispenseMethod", "PHARMACY");
        if (overrideReason != null) {
            body.put("allergyOverrideReason", overrideReason);
        }
        return json.writeValueAsString(body);
    }

    private String login(String staffNumber, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
