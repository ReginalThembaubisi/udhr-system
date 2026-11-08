package com.udhr.safety;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Patient;
import com.udhr.repository.LabResultRepository;
import com.udhr.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A request that leaves out the patient's ID number must never land on someone
 * else's record. Found in end-to-end testing: a lab result sent without an ID
 * number failed with "2 results were returned" because the look-up matched
 * every patient without a passport.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PatientLookupTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;
    @Autowired private LabResultRepository labResultRepository;

    @Test
    void aMissingIdNumberMatchesNobody() throws Exception {
        // A patient registered with a passport and no SA ID number.
        Patient visitor = new Patient();
        visitor.setFirstName("Ana");
        visitor.setLastName("Lookuptest");
        visitor.setPassportNumber("PLOOKUP01");
        visitor.setDateOfBirth(LocalDate.of(1988, 2, 2));
        visitor.setGender("FEMALE");
        visitor.setMrn("MRN-LOOKUP-000001");
        visitor = patientRepository.save(visitor);

        assertThat(patientRepository.findByIdNumber(null)).isEmpty();
        assertThat(patientRepository.existsByIdNumber(null)).isFalse();
        assertThat(patientRepository.findByPassportNumber(null)).isEmpty();
        assertThat(patientRepository.findByPassportNumber("PLOOKUP01")).map(Patient::getId).contains(visitor.getId());

        String nurse = login("NUR001", "Nurse@123");
        Map<String, Object> lab = new HashMap<>();
        lab.put("testName", "Lookup test");
        lab.put("result", "1");
        lab.put("testDate", "2026-11-08");
        mvc.perform(post("/api/lab-results").header("Authorization", "Bearer " + nurse)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(lab)))
                .andExpect(status().isBadRequest());
        assertThat(labResultRepository.findByPatientIdOrderByTestDateDesc(visitor.getId())).isEmpty();
    }

    private String login(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.5.0.1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
