package com.udhr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.StockTransaction;
import com.udhr.repository.StockTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stock changes need a reason, absurd numbers are refused, and a shelf count sets the exact number. */
@SpringBootTest
@AutoConfigureMockMvc
class StockUpdateTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StockTransactionRepository transactions;

    @Test
    void stockChangesAreCheckedAndExplained() throws Exception {
        String pharm = login("PHARM001", "Pharmacist@123");
        long id = json.readTree(mvc.perform(post("/api/stock").header("Authorization", "Bearer " + pharm)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "medicationName", "Zentrocil Teststock", "unit", "tablets", "quantityOnHand", 200, "reorderLevel", 20))))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // A typing mistake like 500001 is refused, when adding a new item too.
        mvc.perform(post("/api/stock").header("Authorization", "Bearer " + pharm).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("medicationName", "Hugecil Teststock", "unit", "tablets", "quantityOnHand", 500001))))
                .andExpect(status().isBadRequest());
        adjust(pharm, id, 50000, "Delivery received").andExpect(status().isBadRequest());
        mvc.perform(post("/api/stock/receive").header("Authorization", "Bearer " + pharm).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("stockItemId", id, "quantityChange", 500001))))
                .andExpect(status().isBadRequest());

        // Removing stock needs a reason, and the reason is kept.
        adjust(pharm, id, -5, "").andExpect(status().isBadRequest());
        adjust(pharm, id, -5, "Damaged").andExpect(status().isOk()).andExpect(jsonPath("$.quantityOnHand").value(195));
        List<StockTransaction> history = transactions.findByStockItemIdOrderByCreatedAtDesc(id);
        assertThat(history.get(0).getNotes()).isEqualTo("Damaged");

        // "I counted the shelf": exact number, difference recorded.
        mvc.perform(post("/api/stock/" + id + "/count").header("Authorization", "Bearer " + pharm)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"countedQuantity\":180}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantityOnHand").value(180));
        assertThat(transactions.findByStockItemIdOrderByCreatedAtDesc(id).get(0).getNotes()).contains("counted 180", "had 195");

        // Fix the expiry date and reorder level without touching the quantity.
        mvc.perform(put("/api/stock/" + id).header("Authorization", "Bearer " + pharm).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiryDate\":\"2028-01-31\",\"reorderLevel\":30}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expiryDate").value("2028-01-31"))
                .andExpect(jsonPath("$.reorderLevel").value(30)).andExpect(jsonPath("$.quantityOnHand").value(180));

        // Only a pharmacist can count or edit.
        String nurse = login("NUR001", "Nurse@123");
        mvc.perform(post("/api/stock/" + id + "/count").header("Authorization", "Bearer " + nurse)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"countedQuantity\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/stock/" + id).header("Authorization", "Bearer " + nurse).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reorderLevel\":1}"))
                .andExpect(status().isForbidden());
    }

    private ResultActions adjust(String token, long id, int change, String notes) throws Exception {
        return mvc.perform(post("/api/stock/adjust").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("stockItemId", id, "quantityChange", change, "notes", notes))));
    }

    private String login(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.11.0.1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
