package com.udhr.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.StockItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A pharmacist loads a whole stock list at once instead of item by item. */
@SpringBootTest
@AutoConfigureMockMvc
class StockBulkImportTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StaffRepository staffRepository;
    @Autowired private StockItemRepository stockItemRepository;

    @Test
    void aListIsAddedInOneGoAndTopsUpWhatIsAlreadyStocked() throws Exception {
        String pharmacist = login("PHARM001", "Pharmacist@123");
        Long facilityId = staffRepository.findByStaffNumber("PHARM001").orElseThrow().getFacility().getId();
        String expiry = LocalDate.now().plusYears(1).toString();

        JsonNode first = json.readTree(send(pharmacist, List.of(
                row("Bulk Test Paracetamol 500mg", "tablets", 1000, 200, expiry),
                row("Bulk Test Amoxicillin 500mg", "capsules", 0, 50, null)), 200));
        assertThat(first.get("created").asInt()).isEqualTo(2);

        // The same medicine again (any case) adds to it rather than duplicating it.
        JsonNode second = json.readTree(send(pharmacist, List.of(
                row("bulk test paracetamol 500mg", null, 500, null, null),
                row("Bulk Test Cetirizine 10mg", "tablets", 60, null, null)), 200));
        assertThat(second.get("created").asInt()).isEqualTo(1);
        assertThat(second.get("toppedUp").asInt()).isEqualTo(1);
        var paracetamol = stockItemRepository.findByFacilityIdAndMedicationNameIgnoreCase(facilityId, "Bulk Test Paracetamol 500mg").orElseThrow();
        assertThat(paracetamol.getQuantityOnHand()).isEqualTo(1500);
        assertThat(paracetamol.getReorderLevel()).isEqualTo(200);
        assertThat(paracetamol.getExpiryDate()).isEqualTo(LocalDate.parse(expiry));
    }

    @Test
    void oneBadRowStopsTheWholeListAndEveryProblemIsNamed() throws Exception {
        String pharmacist = login("PHARM001", "Pharmacist@123");
        Long facilityId = staffRepository.findByStaffNumber("PHARM001").orElseThrow().getFacility().getId();

        JsonNode refused = json.readTree(send(pharmacist, List.of(
                row("Bulk Bad Ibuprofen 400mg", "tablets", 100, 20, null),
                row("", "tablets", 5, null, null),
                row("Bulk Bad Expired", "tablets", 10, null, LocalDate.now().minusDays(3).toString()),
                row("bulk bad ibuprofen 400mg", "tablets", 1, null, null),
                row("Bulk Bad Negative", "tablets", -4, null, null)), 400));
        assertThat(refused.get("errors")).hasSize(4);
        assertThat(refused.get("errors").toString())
                .contains("Row 2").contains("missing").contains("expired").contains("already in row 1").contains("negative");
        // Nothing was saved, not even the good first row.
        assertThat(stockItemRepository.findByFacilityIdAndMedicationNameIgnoreCase(facilityId, "Bulk Bad Ibuprofen 400mg")).isEmpty();
    }

    @Test
    void onlyThePharmacistLoadsStockLists() throws Exception {
        String doctor = login("DOC001", "Doctor@123");
        send(doctor, List.of(row("Bulk Doctor Item", "tablets", 1, null, null)), 403);
    }

    private String send(String token, List<Map<String, Object>> rows, int expectedStatus) throws Exception {
        return mvc.perform(post("/api/stock/bulk").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(rows)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private static Map<String, Object> row(String name, String unit, Integer quantity, Integer reorder, String expiry) {
        Map<String, Object> row = new HashMap<>();
        row.put("medicationName", name);
        row.put("unit", unit);
        row.put("quantityOnHand", quantity);
        row.put("reorderLevel", reorder);
        row.put("expiryDate", expiry);
        return row;
    }

    private String login(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.6.0.1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
