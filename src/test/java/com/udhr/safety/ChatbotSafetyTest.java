package com.udhr.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The patient health assistant, tested with the messages it got wrong in the
 * October 2026 audit (fix list P0-9, P0-10, P2-13).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatbotSafetyTest {

    private static final String GREETING = "Hi! I'm your health assistant";
    private static final String SUICIDE_LINE = "0800 567 567";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private String patientToken;

    @BeforeEach
    void loginAsSeededPatient() throws Exception {
        String body = mvc.perform(post("/api/auth/patient/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idNumber", "9001015000083", "dateOfBirth", "1990-01-01"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        patientToken = json.readTree(body).get("token").asText();
    }

    // ---- P0-9: self-harm and emergencies ------------------------------------------

    @Test
    void selfHarmGetsCrisisSupport() throws Exception {
        for (String message : new String[] {
                "I'm thinking about suicide",
                "I want to end my life",
                "I don't want to live anymore",
                "sometimes I want to hurt myself"}) {
            JsonNode reply = ask(message);
            assertThat(reply.get("urgencyLevel").asText()).as(message).isEqualTo("RED");
            assertThat(reply.get("reply").asText()).as(message).contains(SUICIDE_LINE).contains("10177");
        }
    }

    @Test
    void emergenciesGetTheEmergencyReply() throws Exception {
        for (String message : new String[] {
                "I'm pregnant and bleeding",
                "I have crushing chest pain",
                "my father collapsed",
                "I can't breathe"}) {
            JsonNode reply = ask(message);
            assertThat(reply.get("urgencyLevel").asText()).as(message).isEqualTo("RED");
            assertThat(reply.get("reply").asText()).as(message).contains("10177");
        }
    }

    // ---- P0-10: greetings only for greetings --------------------------------------

    @Test
    void realQuestionsContainingHiAreNotTreatedAsGreetings() throws Exception {
        assertThat(ask("Which medication should I avoid?").get("reply").asText())
                .doesNotContain(GREETING).contains("Amoxicillin").doesNotContain("Unknown");
        assertThat(ask("I am HIV positive and on ARVs, what food should I avoid?").get("reply").asText()).doesNotContain(GREETING);

        JsonNode rash = ask("My child has a rash");
        assertThat(rash.get("reply").asText()).doesNotContain(GREETING);
        assertThat(rash.get("matchedSymptoms").toString()).contains("Skin Rash");
    }

    @Test
    void plainGreetingsStillGetTheGreeting() throws Exception {
        assertThat(ask("hi").get("reply").asText()).contains(GREETING);
        assertThat(ask("Hello!").get("reply").asText()).contains(GREETING);
        assertThat(ask("good morning").get("reply").asText()).contains(GREETING);
    }

    // ---- P2-13: no false triage, safe answer on interactions ----------------------

    @Test
    void negatedOrUnrelatedMessagesCreateNoTriageRecord() throws Exception {
        int before = triageHistorySize();

        JsonNode negated = ask("I don't have a fever, just checking my pills");
        assertThat(negated.get("urgencyLevel").isNull()).isTrue();
        JsonNode insulin = ask("What temperature should I store my insulin at?");
        assertThat(insulin.get("urgencyLevel").isNull()).isTrue();
        JsonNode sweat = ask("I'm sweating a lot");
        assertThat(sweat.get("matchedSymptoms").toString()).contains("Sweating");

        // Only the sweating message is a real symptom report.
        assertThat(triageHistorySize()).isEqualTo(before + 1);
    }

    @Test
    void realSymptomsAreStillPickedUp() throws Exception {
        assertThat(ask("I think I have a fever").get("matchedSymptoms").toString()).contains("Mild Fever");
    }

    @Test
    void interactionQuestionsAreSentToThePharmacist() throws Exception {
        String reply = ask("Can I take ibuprofen with my metformin?").get("reply").asText();
        assertThat(reply).contains("pharmacist").contains("Metformin");
    }

    // ---- helpers ------------------------------------------------------------------

    private JsonNode ask(String message) throws Exception {
        String body = mvc.perform(post("/api/patient/chatbot").header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("message", message))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private int triageHistorySize() throws Exception {
        String body = mvc.perform(get("/api/symptom-checker/history").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).size();
    }
}
