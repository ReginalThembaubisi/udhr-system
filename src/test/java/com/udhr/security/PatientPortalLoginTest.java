package com.udhr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Patient;
import com.udhr.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fix list P0-7: the patient portal needs a PIN set at the clinic, not just an
 * ID number and the date of birth that the ID number already contains.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PatientPortalLoginTest {

    private static final String DEMO_ID = "9001015000083";
    private static final String DEMO_DOB = "1990-01-01";
    private static final String DEMO_PIN = "246810"; // seeded on a fresh database

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;

    @Test
    void idAndDateOfBirthAloneNoLongerSignIn() throws Exception {
        patientLogin(DEMO_ID, DEMO_DOB, null).andExpect(status().isUnauthorized());
        patientLogin(DEMO_ID, DEMO_DOB, "000000").andExpect(status().isUnauthorized());
        patientLogin(DEMO_ID, DEMO_DOB, DEMO_PIN).andExpect(status().isOk());
    }

    @Test
    void wrongDetailsAllGetTheSameMessage() throws Exception {
        String unknownId = body(patientLogin("0000000000000", DEMO_DOB, DEMO_PIN).andExpect(status().isUnauthorized()));
        String wrongDob = body(patientLogin(DEMO_ID, "1991-01-01", DEMO_PIN).andExpect(status().isUnauthorized()));
        assertThat(unknownId).isEqualTo(wrongDob);
    }

    @Test
    void fiveWrongPinsLockTheAccountEvenAgainstTheRightPin() throws Exception {
        String id = newPatient("8001015009087");
        frontDeskSetsPin(id, "731946").andExpect(status().isOk());

        for (int i = 0; i < 5; i++) {
            patientLogin(id, "1980-01-01", "111111").andExpect(status().isUnauthorized());
        }
        String locked = body(patientLogin(id, "1980-01-01", "731946").andExpect(status().isUnauthorized()));
        assertThat(locked).contains("Too many incorrect attempts");

        // The front desk resetting the PIN also clears the lock.
        frontDeskSetsPin(id, "582047").andExpect(status().isOk());
        patientLogin(id, "1980-01-01", "582047").andExpect(status().isOk());
    }

    @Test
    void patientWithoutAPinIsToldToAskTheFrontDesk() throws Exception {
        String id = newPatient("8505055800081");
        String reply = body(patientLogin(id, "1980-01-01", "246810").andExpect(status().isUnauthorized()));
        assertThat(reply).contains("front desk");
    }

    @Test
    void onlyTheFrontDeskSetsPins_andWeakPinsAreRefused() throws Exception {
        String id = newPatient("7001015009083");
        frontDeskSetsPin(id, "123456").andExpect(status().isBadRequest());
        frontDeskSetsPin(id, "999999").andExpect(status().isBadRequest());
        frontDeskSetsPin(id, "12345").andExpect(status().isBadRequest());

        String nurse = staffLogin("NUR001", "Nurse@123");
        mvc.perform(post("/api/checkin/portal-pin").header("Authorization", "Bearer " + nurse)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("identifier", id, "pin", "730519"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void frontDeskLookupShowsWhetherAPinIsSetButNeverTheHash() throws Exception {
        String admin = staffLogin("ADMIN001", "Admin@123");
        String lookup = mvc.perform(get("/api/checkin/lookup/" + DEMO_ID).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(lookup).contains("\"portalPinSet\":true").doesNotContain("portalPinHash").doesNotContain("$2a$");
    }

    // ---- helpers ------------------------------------------------------------------

    private String newPatient(String idNumber) {
        return patientRepository.findByIdNumber(idNumber).map(Patient::getIdNumber).orElseGet(() -> {
            Patient p = new Patient();
            p.setIdNumber(idNumber);
            p.setMrn("MRN-TEST-" + idNumber.substring(0, 6));
            p.setFirstName("Test");
            p.setLastName("Patient");
            p.setDateOfBirth(LocalDate.of(1980, 1, 1));
            p.setGender("FEMALE");
            return patientRepository.save(p).getIdNumber();
        });
    }

    private ResultActions patientLogin(String id, String dob, String pin) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("idNumber", id);
        body.put("dateOfBirth", dob);
        if (pin != null) body.put("pin", pin);
        return mvc.perform(post("/api/auth/patient/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private ResultActions frontDeskSetsPin(String id, String pin) throws Exception {
        String admin = staffLogin("ADMIN001", "Admin@123");
        return mvc.perform(post("/api/checkin/portal-pin").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identifier", id, "pin", pin))));
    }

    private String staffLogin(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }
}
