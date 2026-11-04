package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Patient;
import com.udhr.repository.PatientRepository;
import com.udhr.service.PortalPinGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** New registrations issue a one-time PIN and restrict access until it changes. */
@SpringBootTest
@AutoConfigureMockMvc
class FirstLoginCredentialsTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patients;
    @Autowired private BCryptPasswordEncoder encoder;

    @Test
    void generatedPinsHaveSixDigitsIncludingLeadingZeroWhenNeeded() {
        for (int i = 0; i < 250; i++) {
            String pin = PortalPinGenerator.generate();
            assertThat(pin).matches("[0-9]{6}");
            assertThat(pin.chars().distinct().count()).isNotEqualTo(1L);
        }
    }

    @Test
    void newPatientHasOneTimeTemporaryPinAndMustReplaceItBeforeSeeingRecords() throws Exception {
        String admin = login("ADMIN001", "Admin@123").get("token").asText();
        String id = "7212025000187";
        String request = json.writeValueAsString(Map.of(
                "idNumber", id, "firstName", "PIN", "lastName", "Newpatient",
                "dateOfBirth", "1972-12-02", "gender", "FEMALE",
                "privacyNoticeAccepted", true));
        JsonNode registered = json.readTree(mvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        String temporaryPin = registered.get("temporaryPortalPin").asText();
        assertThat(temporaryPin).matches("[0-9]{6}");
        assertThat(registered.toString()).doesNotContain("portalPinHash");
        Patient stored = patients.findByIdNumber(id).orElseThrow();
        assertThat(encoder.matches(temporaryPin, stored.getPortalPinHash())).isTrue();
        assertThat(stored.getMustChangePortalPin()).isTrue();

        JsonNode session = patientLogin(id, "1972-12-02", temporaryPin);
        assertThat(session.get("mustChangePin").asBoolean()).isTrue();
        String restrictedToken = session.get("token").asText();
        mvc.perform(get("/api/patient/me/record").header("Authorization", "Bearer " + restrictedToken))
                .andExpect(status().isForbidden());

        // The temporary PIN cannot be reused as the new permanent PIN.
        mvc.perform(post("/api/patient/me/change-pin")
                .header("Authorization", "Bearer " + restrictedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("currentPin", temporaryPin, "newPin", temporaryPin))))
                .andExpect(status().isBadRequest());

        String newPin = temporaryPin.equals("752946") ? "863175" : "752946";
        JsonNode changed = json.readTree(mvc.perform(post("/api/patient/me/change-pin")
                .header("Authorization", "Bearer " + restrictedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("currentPin", temporaryPin, "newPin", newPin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(changed.get("mustChangePin").asBoolean()).isFalse();
        mvc.perform(get("/api/patient/me").header("Authorization", "Bearer " + changed.get("token").asText()))
                .andExpect(status().isOk());
        assertThat(patients.findByIdNumber(id).orElseThrow().getMustChangePortalPin()).isFalse();
        // Temporary PIN is no longer valid after changing it.
        mvc.perform(post("/api/auth/patient/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idNumber", id,
                        "dateOfBirth", "1972-12-02", "pin", temporaryPin))))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode patientLogin(String id, String dob, String pin) throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/patient/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idNumber", id, "dateOfBirth", dob, "pin", pin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode login(String staffNumber, String password) throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
}
