package com.udhr.privacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Patient;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POPIA protections: consent before registration, logging every read of a
 * patient's data, and reports that don't identify patients.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PopiaProtectionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    // ---- Consent (sections 18, 26-27, 35) ---------------------------------------

    @Test
    void registrationNeedsTheNoticeExplainedAndAgreed() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        String message = register(admin, "5505055000086", "1955-05-05", null, null)
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(message).contains("privacy notice");

        register(admin, "5505055000086", "1955-05-05", true, null).andExpect(status().isCreated());
        Patient saved = patientRepository.findByIdNumber("5505055000086").orElseThrow();
        assertThat(saved.getPrivacyNoticeAcceptedAt()).isNotNull();
        assertThat(saved.getPrivacyNoticeVersion()).isNotBlank();
        assertThat(saved.getConsentGivenBy()).isEqualTo("Patient");
        assertThat(saved.getSmsRemindersConsent()).isFalse();
    }

    @Test
    void aChildNeedsAParentOrGuardianNamed() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        register(admin, "1801015000080", "2018-01-01", true, null).andExpect(status().isBadRequest());
        register(admin, "1801015000080", "2018-01-01", true, "Mother: Thandi Zulu").andExpect(status().isCreated());
        assertThat(patientRepository.findByIdNumber("1801015000080").orElseThrow().getConsentGivenBy())
                .isEqualTo("Mother: Thandi Zulu");
    }

    @Test
    void frontDeskCanRecordConsentAndPatientsCanSwitchSmsOff() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        mvc.perform(post("/api/checkin/consent").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("identifier", "9001015000083", "smsRemindersConsent", true))))
                .andExpect(status().isOk());

        String body = mvc.perform(post("/api/auth/patient/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idNumber", "9001015000083", "dateOfBirth", "1990-01-01", "pin", "246810"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String patient = json.readTree(body).get("token").asText();
        mvc.perform(put("/api/patient/me/sms-reminders").header("Authorization", "Bearer " + patient)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk());
        assertThat(patientRepository.findByIdNumber("9001015000083").orElseThrow().getSmsRemindersConsent()).isFalse();
        assertThat(auditLogRepository.findAllByOrderByCreatedAtDesc())
                .anyMatch(log -> "PATIENT_SMS_CONSENT".equals(log.getAction()))
                .anyMatch(log -> "RECORD_CONSENT".equals(log.getAction()));
    }

    // ---- Every read of patient data is logged (sections 8 and 19) -----------------

    @Test
    void aNurseReadingVitalsIsLoggedOncePerSitting() throws Exception {
        String nurse = login("NUR001", "Nurse@123");
        Long patientId = patientRepository.findByIdNumber("9001015000083").orElseThrow().getId();
        long before = countViews("vitals");

        mvc.perform(get("/api/vitals/patient/" + patientId).header("Authorization", "Bearer " + nurse)).andExpect(status().isOk());
        mvc.perform(get("/api/vitals/patient/" + patientId).header("Authorization", "Bearer " + nurse)).andExpect(status().isOk());

        assertThat(countViews("vitals")).isEqualTo(before + 1);
    }

    // ---- Reports don't identify patients (section 10) -----------------------------

    @Test
    void managementReportsCarryNoPatientIdentity() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        for (String path : new String[] {"/api/prescriptions/report", "/api/prescriptions/report/export",
                "/api/dispensing/report", "/api/dispensing/report/export",
                "/api/lab-results/report", "/api/lab-results/report/export"}) {
            String body = mvc.perform(get(path).header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(body).as(path).doesNotContain("9001015000083").doesNotContain("Mandela Drive")
                    .doesNotContain("0821234567").doesNotContain("\"patient\"");
        }
        String csv = mvc.perform(get("/api/prescriptions/report/export").header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        assertThat(csv.lines().findFirst().orElse("")).doesNotContain("Patient").doesNotContain("Notes");
    }

    // ---- helpers ------------------------------------------------------------------

    private long countViews(String area) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(log -> "VIEW_PATIENT_DATA".equals(log.getAction()) && log.getDescription().contains(area))
                .count();
    }

    private ResultActions register(String token, String idNumber, String dateOfBirth, Boolean accepted, String givenBy) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("idNumber", idNumber);
        body.put("firstName", "Popia");
        body.put("lastName", "Test" + idNumber.substring(0, 4));
        body.put("dateOfBirth", dateOfBirth);
        body.put("gender", "FEMALE");
        if (accepted != null) body.put("privacyNoticeAccepted", accepted);
        if (givenBy != null) body.put("consentGivenBy", givenBy);
        return mvc.perform(post("/api/patients").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String login(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
