package com.udhr.ux;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Front desk and nurse behaviour that used to confuse staff (fix list P2):
 * duplicate registrations, double check-ins, impossible vital signs and the
 * portal's empty referral response.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FrontDeskAndVitalsTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void sameNameAndBirthDateAsksBeforeCreatingASecondFile() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        mvc.perform(post("/api/patients").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patient("8505055000081", null)))
                .andExpect(status().isCreated());

        JsonNode conflict = json.readTree(mvc.perform(post("/api/patients").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patient("8505055000082", null)))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString());
        assertThat(conflict.get("possibleDuplicate").get("idNumber").asText()).isEqualTo("8505055000081");

        // The front desk checked: a different person who happens to share the details.
        mvc.perform(post("/api/patients").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patient("8505055000082", true)))
                .andExpect(status().isCreated());
    }

    @Test
    void checkingInTwiceKeepsOneVisitInTheQueue() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        mvc.perform(post("/api/patients").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idNumber", "7707075000083", "firstName", "Lerato",
                                "lastName", "Mokoena", "dateOfBirth", "1977-07-07", "gender", "FEMALE", "privacyNoticeAccepted", true))))
                .andExpect(status().isCreated());

        JsonNode first = checkIn(admin, "Headache", 201);
        assertThat(first.get("alreadyWaiting").asBoolean()).isFalse();
        JsonNode second = checkIn(admin, "Headache and fever", 200);
        assertThat(second.get("alreadyWaiting").asBoolean()).isTrue();
        assertThat(second.get("visit").get("id").asLong()).isEqualTo(first.get("visit").get("id").asLong());
    }

    @Test
    void impossibleVitalsAreRefusedWithAPlainMessage() throws Exception {
        String nurse = login("NUR001", "Nurse@123");
        Map<String, Object> vitals = new HashMap<>();
        vitals.put("idNumber", "9001015000083");
        vitals.put("bloodPressure", "1200/80");
        vitals.put("temperatureCelsius", 370.0);
        String message = mvc.perform(post("/api/vitals").header("Authorization", bearer(nurse))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(vitals)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(message).contains("Please check the readings").contains("blood pressure").contains("emperature");
    }

    @Test
    void portalWithoutAReferralGetsNoContent() throws Exception {
        // A patient of its own: other tests refer the seeded patient.
        String admin = login("ADMIN001", "Admin@123");
        mvc.perform(post("/api/patients").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idNumber", "6606065000084", "firstName", "Thandi",
                                "lastName", "Zulu", "dateOfBirth", "1966-06-06", "gender", "FEMALE", "portalPin", "583920",
                                "privacyNoticeAccepted", true))))
                .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/auth/patient/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idNumber", "6606065000084", "dateOfBirth", "1966-06-06", "pin", "583920"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("token").asText();
        mvc.perform(get("/api/patient/me/referral").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    // ---- helpers ------------------------------------------------------------------

    private JsonNode checkIn(String admin, String reason, int expectedStatus) throws Exception {
        return json.readTree(mvc.perform(post("/api/checkin").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idNumber", "7707075000083", "reason", reason))))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString());
    }

    private String patient(String idNumber, Boolean confirmNotDuplicate) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("idNumber", idNumber);
        body.put("firstName", "Sipho");
        body.put("lastName", "Dlamini");
        body.put("dateOfBirth", "1985-05-05");
        body.put("gender", "MALE");
        body.put("privacyNoticeAccepted", true);
        if (confirmNotDuplicate != null) {
            body.put("confirmNotDuplicate", confirmNotDuplicate);
        }
        return json.writeValueAsString(body);
    }

    private String login(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
