package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Patient;
import com.udhr.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A patient with no SA ID number (a passport holder) signs in with their file number or passport and uses the portal. */
@SpringBootTest
@AutoConfigureMockMvc
class PatientWithoutSaIdTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Test
    void aPassportHolderSignsInWithTheirFileNumberOrPassport() throws Exception {
        Patient p = new Patient();
        p.setFirstName("Amara");
        p.setLastName("Passportholder");
        p.setPassportNumber("ZW1234567");
        p.setMrn("MRN-PASSPORT-000001");
        p.setDateOfBirth(LocalDate.of(1992, 7, 14));
        p.setGender("FEMALE");
        p.setPortalPinHash(passwordEncoder.encode("583920"));
        patientRepository.save(p);

        for (String identifier : new String[]{"MRN-PASSPORT-000001", "ZW1234567"}) {
            JsonNode session = json.readTree(mvc.perform(post("/api/auth/patient/login").header("X-Forwarded-For", "10.13.0.1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("idNumber", identifier, "dateOfBirth", "1992-07-14", "pin", "583920"))))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            String token = session.get("token").asText();
            // The session works for their own record, not just the sign-in.
            String me = mvc.perform(get("/api/patient/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(me).contains("Passportholder");
            mvc.perform(get("/api/patient/me/record").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }
}
