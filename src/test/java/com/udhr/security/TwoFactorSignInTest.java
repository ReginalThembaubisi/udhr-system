package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Staff;
import com.udhr.repository.StaffRepository;
import com.udhr.service.TotpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two-step sign-in with an authenticator app, recovery by the system admin,
 * and the "last sign-in" notice. Each test uses its own account and address.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TwoFactorSignInTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StaffRepository staffRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Test
    void withAnAuthenticatorThePasswordAloneIsNotEnough() throws Exception {
        newStaff("DOC701", "Doctor@701");
        String token = token(login("DOC701", "Doctor@701", "10.1.1.1"));

        String secret = json.readTree(mvc.perform(post("/api/auth/2fa/setup").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("secret").asText();
        long now = System.currentTimeMillis() / 1000 / 30;
        mvc.perform(post("/api/auth/2fa/confirm").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("code", TotpService.codeAt(secret, now)))))
                .andExpect(status().isOk());
        // Turning it on ended the sessions that existed before.
        mvc.perform(get("/api/visits/today").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());

        // Password right: no session yet, only a challenge.
        MockHttpServletResponse first = login("DOC701", "Doctor@701", "10.1.1.2").andExpect(status().isOk()).andReturn().getResponse();
        JsonNode challenge = json.readTree(first.getContentAsString());
        assertThat(challenge.get("twoFactorRequired").asBoolean()).isTrue();
        assertThat(challenge.get("token").isNull()).isTrue();
        assertThat(first.getHeader("Set-Cookie")).isNull();
        String challengeToken = challenge.get("challengeToken").asText();

        // The challenge itself is not a sign-in.
        mvc.perform(get("/api/visits/today").header("Authorization", "Bearer " + challengeToken)).andExpect(status().isUnauthorized());

        verify(challengeToken, "000000", "10.1.1.2").andExpect(status().isUnauthorized());
        // The code used to turn it on can't be used again; the next one works.
        verify(challengeToken, TotpService.codeAt(secret, now), "10.1.1.2").andExpect(status().isUnauthorized());
        String signedIn = verify(challengeToken, TotpService.codeAt(secret, now + 1), "10.1.1.2")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/visits/today").header("Authorization", "Bearer " + json.readTree(signedIn).get("token").asText()))
                .andExpect(status().isOk());
        // And the same code can't be replayed.
        verify(challengeToken, TotpService.codeAt(secret, now + 1), "10.1.1.2").andExpect(status().isUnauthorized());
    }

    @Test
    void theSystemAdminCanResetALostAuthenticator() throws Exception {
        Staff staff = newStaff("DOC702", "Doctor@702");
        String token = token(login("DOC702", "Doctor@702", "10.1.2.1"));
        String secret = json.readTree(mvc.perform(post("/api/auth/2fa/setup").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("secret").asText();
        mvc.perform(post("/api/auth/2fa/confirm").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", TotpService.codeAt(secret, System.currentTimeMillis() / 1000 / 30)))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/system-admin/staff/" + staff.getId() + "/reset-two-factor")
                        .header("Authorization", "Bearer " + token(login("SYSADMIN001", "SysAdmin@123", "10.1.2.2"))))
                .andExpect(status().isOk());

        JsonNode after = json.readTree(login("DOC702", "Doctor@702", "10.1.2.3").andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(after.get("token").isNull()).isFalse();
    }

    @Test
    void resettingThePasswordLiftsTheLockout() throws Exception {
        Staff staff = newStaff("NUR703", "Nurse@703x");
        for (int i = 0; i < 5; i++) {
            login("NUR703", "wrong-password", "10.1.3." + i).andExpect(status().isUnauthorized());
        }
        login("NUR703", "Nurse@703x", "10.1.3.9").andExpect(status().isTooManyRequests());

        String sysAdmin = token(login("SYSADMIN001", "SysAdmin@123", "10.1.3.20"));
        String reset = mvc.perform(post("/api/system-admin/staff/" + staff.getId() + "/reset-password")
                        .header("Authorization", "Bearer " + sysAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String temporary = json.readTree(reset).get("temporaryPassword").asText();
        login("NUR703", temporary, "10.1.3.21").andExpect(status().isOk());
    }

    @Test
    void signingInShowsTheLastSignInAndFailedAttemptsSince() throws Exception {
        newStaff("PHARM704", "Pharm@704x");
        login("PHARM704", "Pharm@704x", "10.1.4.1").andExpect(status().isOk());
        login("PHARM704", "wrong-password", "10.1.4.2").andExpect(status().isUnauthorized());

        JsonNode second = json.readTree(login("PHARM704", "Pharm@704x", "10.1.4.3").andReturn().getResponse().getContentAsString());
        assertThat(second.get("lastSignInAt").isNull()).isFalse();
        assertThat(second.get("lastSignInFrom").asText()).isEqualTo("10.1.4.1");
        assertThat(second.get("failedAttemptsSinceLastSignIn").asLong()).isEqualTo(1);
    }

    // ---- helpers ------------------------------------------------------------------

    private ResultActions login(String staffNumber, String password, String address) throws Exception {
        return mvc.perform(post("/api/auth/login").header("X-Forwarded-For", address).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))));
    }

    private ResultActions verify(String challengeToken, String code, String address) throws Exception {
        return mvc.perform(post("/api/auth/login/verify").header("X-Forwarded-For", address).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("challengeToken", challengeToken, "code", code))));
    }

    private String token(ResultActions login) throws Exception {
        return json.readTree(login.andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
    }

    private Staff newStaff(String staffNumber, String password) {
        Staff staff = new Staff();
        staff.setStaffNumber(staffNumber);
        staff.setFirstName("Test");
        staff.setLastName("Staff");
        staff.setRole(staffNumber.startsWith("DOC") ? "DOCTOR" : staffNumber.startsWith("NUR") ? "NURSE" : "PHARMACIST");
        staff.setFacility(staffRepository.findByStaffNumber("NUR001").orElseThrow().getFacility());
        staff.setEmail(staffNumber.toLowerCase() + "@test.local");
        staff.setPassword(passwordEncoder.encode(password));
        staff.setActive(true);
        staff.setMustChangePassword(false);
        return staffRepository.save(staff);
    }
}
