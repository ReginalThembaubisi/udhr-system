package com.udhr.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * The prescriber sees what the facility's pharmacy has, and gets in-stock
 * alternatives that the patient isn't allergic to. The seeded patient
 * 9001015000083 has a severe penicillin allergy.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StockAvailabilityTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;

    private static boolean stocked;

    @BeforeEach
    void stockThePharmacy() throws Exception {
        if (stocked) return;
        String pharmacist = login("PHARM001", "Pharmacist@123");
        addStock(pharmacist, "Panado 500mg", 0);
        addStock(pharmacist, "Calpol syrup", 40);
        addStock(pharmacist, "Azithromycin 250mg", 0);
        addStock(pharmacist, "Clarithromycin 500mg", 30);
        addStock(pharmacist, "Augmentin 625mg", 50);
        addStock(pharmacist, "TLD", 0);
        addStock(pharmacist, "Efavirenz 600mg", 20);
        stocked = true;
    }

    @Test
    void anotherBrandOfTheSameMedicineCountsAsInStock() throws Exception {
        JsonNode result = check("Paracetamol 500mg", null);
        assertThat(result.get("status").asText()).isEqualTo("IN_STOCK");
        assertThat(result.get("stockName").asText()).isEqualTo("Calpol syrup");
    }

    @Test
    void outOfStockOffersTheSameClassInStock() throws Exception {
        JsonNode result = check("Azithromycin 250mg", patientId());
        assertThat(result.get("status").asText()).isEqualTo("OUT_OF_STOCK");
        assertThat(result.get("alternatives").toString()).contains("Clarithromycin").contains("SAME_CLASS");
    }

    @Test
    void neverSuggestsSomethingThePatientIsAllergicTo() throws Exception {
        assertThat(check("Amoxicillin 500mg", null).get("alternatives").toString()).contains("Augmentin");
        JsonNode forAllergicPatient = check("Amoxicillin 500mg", patientId());
        assertThat(forAllergicPatient.get("status").asText()).isEqualTo("NOT_STOCKED");
        assertThat(forAllergicPatient.get("alternatives").toString()).doesNotContain("Augmentin");
        assertThat(forAllergicPatient.get("leftOutForAllergy").toString()).contains("Augmentin");
    }

    @Test
    void antiretroviralsGetNoSwapSuggestions() throws Exception {
        JsonNode result = check("TLD", null);
        assertThat(result.get("status").asText()).isEqualTo("OUT_OF_STOCK");
        assertThat(result.get("alternatives")).isEmpty();
    }

    @Test
    void theFrontDeskCannotQueryStock() throws Exception {
        mvc.perform(get("/api/stock/availability").param("medication", "Panado")
                        .header("Authorization", "Bearer " + login("ADMIN001", "Admin@123")))
                .andExpect(status().isForbidden());
    }

    private JsonNode check(String medication, Long patientId) throws Exception {
        var request = get("/api/stock/availability").param("medication", medication)
                .header("Authorization", "Bearer " + login("DOC001", "Doctor@123"));
        if (patientId != null) request = request.param("patientId", String.valueOf(patientId));
        return json.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private Long patientId() {
        return patientRepository.findByIdNumber("9001015000083").orElseThrow().getId();
    }

    private void addStock(String token, String name, int quantity) throws Exception {
        mvc.perform(post("/api/stock").header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("medicationName", name, "unit", "tablets",
                                "quantityOnHand", quantity, "reorderLevel", 10))))
                .andExpect(status().is2xxSuccessful());
    }

    private String login(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
