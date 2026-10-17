package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Staff;
import com.udhr.model.StockItem;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.StockItemRepository;
import com.udhr.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The questions the panel asked at the presentation, each answered by the
 * running system rather than by a slide:
 *  1. What stops expired medication being handed out?
 *  2. Who resets a locked-out doctor or shuts out someone who has left?
 *  3. Can you see who did what?
 *  4. What if a token is stolen before it expires?
 */
@SpringBootTest
@AutoConfigureMockMvc
class PresentationQuestionsTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StaffRepository staffRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private StockService stockService;

    @Test
    void expiredMedicationIsNeverReceivedOrDispensed() throws Exception {
        Staff pharmacist = staffRepository.findByStaffNumber("PHARM001").orElseThrow();
        StockItem item = new StockItem();
        item.setFacility(pharmacist.getFacility());
        item.setMedicationName("Panel Expiry Check 250mg");
        item.setUnit("tablets");
        item.setQuantityOnHand(100);
        item.setExpiryDate(LocalDate.now().minusDays(1));
        item = stockItemRepository.save(item);

        // Plenty on the shelf, but expired: dispensing is refused, not just warned about.
        assertThatThrownBy(() -> stockService.decrementForDispense(pharmacist.getFacility(),
                "Panel Expiry Check 250mg", "10 tablets", pharmacist, 1L))
                .hasMessageContaining("expired");
        assertThat(stockItemRepository.findById(item.getId()).orElseThrow().getQuantityOnHand()).isEqualTo(100);

        // A delivery that has already expired never reaches the shelf.
        String pharm = login("PHARM001", "Pharmacist@123");
        String refused = mvc.perform(post("/api/stock/receive").header("Authorization", bearer(pharm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("stockItemId", item.getId(), "quantityChange", 50,
                                "expiryDate", LocalDate.now().minusDays(30).toString()))))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(refused).contains("Cannot receive stock that expired");
    }

    @Test
    void theSystemAdminResetsAndShutsOutAccountsAndEveryStepIsInTheAuditTrail() throws Exception {
        String sysAdmin = login("SYSADMIN001", "SysAdmin@123");
        Long facilityId = staffRepository.findByStaffNumber("DOC001").orElseThrow().getFacility().getId();

        // A doctor of its own, so other tests keep DOC001's password.
        mvc.perform(post("/api/staff").header("Authorization", bearer(sysAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", "DOCPANEL", "firstName", "Panel",
                                "lastName", "Doctor", "role", "DOCTOR", "facilityId", facilityId,
                                "email", "panel.doctor@example.org", "password", "Original@123"))))
                .andExpect(status().isCreated());
        Long doctorId = staffRepository.findByStaffNumber("DOCPANEL").orElseThrow().getId();

        // Other staff can't reach any of this.
        String nurse = login("NUR001", "Nurse@123");
        mvc.perform(post("/api/system-admin/staff/" + doctorId + "/reset-password").header("Authorization", bearer(nurse)))
                .andExpect(status().isForbidden());

        // Locked out: the admin issues a one-time password, which must be changed at sign-in.
        String doctorToken = changePassword(login("DOCPANEL", "Original@123"), "Original@123", "Chosen@12345");
        waitASecond();
        JsonNode reset = json.readTree(mvc.perform(post("/api/system-admin/staff/" + doctorId + "/reset-password")
                        .header("Authorization", bearer(sysAdmin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String temporary = reset.get("temporaryPassword").asText();
        signInFails("DOCPANEL", "Chosen@12345");
        JsonNode session = json.readTree(signIn("DOCPANEL", temporary));
        assertThat(session.get("mustChangePassword").asBoolean()).isTrue();
        // The reset also ended the session the doctor (or whoever had it) already had.
        mvc.perform(get("/api/auth/2fa/status").header("Authorization", bearer(doctorToken)))
                .andExpect(status().isUnauthorized());

        // Suspected stolen session: end every session at once.
        String current = changePassword(session.get("token").asText(), temporary, "Second@12345");
        mvc.perform(get("/api/auth/2fa/status").header("Authorization", bearer(current))).andExpect(status().isOk());
        waitASecond();
        mvc.perform(post("/api/system-admin/staff/" + doctorId + "/force-logout").header("Authorization", bearer(sysAdmin)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/2fa/status").header("Authorization", bearer(current)))
                .andExpect(status().isUnauthorized());

        // Left the clinic: deactivated, and the account can't sign in.
        mvc.perform(post("/api/system-admin/staff/" + doctorId + "/active").header("Authorization", bearer(sysAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        signInFails("DOCPANEL", "Second@12345");

        // Who did what, and when: every step above is in the trail, done by SYSADMIN001.
        JsonNode trail = json.readTree(mvc.perform(get("/api/system-admin/audit-log").header("Authorization", bearer(sysAdmin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (String action : new String[] {"CREATE_STAFF_ACCOUNT", "ADMIN_PASSWORD_RESET", "ADMIN_FORCE_LOGOUT", "ADMIN_DEACTIVATED_ACCOUNT"}) {
            JsonNode entry = findEntry(trail, action, "DOCPANEL");
            assertThat(entry).as(action + " recorded").isNotNull();
            assertThat(entry.get("staffNumber").asText()).isEqualTo("SYSADMIN001");
            assertThat(entry.get("createdAt").isNull()).isFalse();
        }
        // The account itself can't read the trail.
        mvc.perform(get("/api/system-admin/audit-log").header("Authorization", bearer(nurse)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aStolenTokenStopsWorkingWhenItsOwnerSignsOut() throws Exception {
        String token = login("PHARM001", "Pharmacist@123");
        mvc.perform(get("/api/auth/2fa/status").header("Authorization", bearer(token))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(token))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/2fa/status").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenSignedWithTheOldPublishedSecretIsRejected() throws Exception {
        // The secret that used to sit in application.properties in this public repository.
        String forged = sign("{\"alg\":\"HS256\"}", "{\"sub\":\"SYSADMIN001\",\"role\":\"SYSTEM_ADMIN\",\"iat\":"
                + (System.currentTimeMillis() / 1000) + ",\"exp\":" + (System.currentTimeMillis() / 1000 + 1800) + "}",
                "udhr-secret-key-2026-university-mpumalanga");
        mvc.perform(get("/api/system-admin/staff").header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    // ---- helpers ------------------------------------------------------------------

    private static JsonNode findEntry(JsonNode trail, String action, String mentions) {
        for (JsonNode entry : trail) {
            if (action.equals(entry.get("action").asText()) && entry.get("description").asText().contains(mentions)) {
                return entry;
            }
        }
        return null;
    }

    private String signIn(String staffNumber, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.9.0.1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void signInFails(String staffNumber, String password) throws Exception {
        mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.9.0.2").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isUnauthorized());
    }

    private String changePassword(String token, String current, String chosen) throws Exception {
        String body = mvc.perform(post("/api/auth/change-password").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("currentPassword", current, "newPassword", chosen))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    // Sessions are cut off to the whole second, so a token is only "earlier"
    // than a reset once a second has passed, as a stolen one would be.
    private static void waitASecond() throws InterruptedException {
        Thread.sleep(1100);
    }

    private String login(String staffNumber, String password) throws Exception {
        return json.readTree(signIn(staffNumber, password)).get("token").asText();
    }

    private static String sign(String header, String payload, String secret) throws Exception {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String unsigned = b64.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return unsigned + "." + b64.encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
