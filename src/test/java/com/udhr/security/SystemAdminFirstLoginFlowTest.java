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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** First-time system administrators must be able to complete BOTH required setup gates. */
@SpringBootTest(properties = {
        "udhr.sysadmin-2fa=required",
        "spring.datasource.url=jdbc:h2:mem:udhr_sysadmin_first_login;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class SystemAdminFirstLoginFlowTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StaffRepository staffRepository;
    @Autowired private BCryptPasswordEncoder encoder;

    @Test
    void newSystemAdminChangesTemporaryPasswordThenSetsUpTwoStepBeforeAccessingAdminFunctions() throws Exception {
        Staff staff = new Staff();
        staff.setStaffNumber("SYSADMIN_FIRST_LOGIN_TEST");
        staff.setFirstName("First");
        staff.setLastName("Login");
        staff.setRole("SYSTEM_ADMIN");
        staff.setFacility(staffRepository.findByStaffNumber("NUR001").orElseThrow().getFacility());
        staff.setEmail("sysadmin.first.login@test.local");
        staff.setPassword(encoder.encode("InitialSecurePass12!"));
        staff.setActive(true);
        staff.setMustChangePassword(true);
        staffRepository.save(staff);

        JsonNode first = json.readTree(mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staff.getStaffNumber(), "password", "InitialSecurePass12!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(first.get("mustChangePassword").asBoolean()).isTrue();
        assertThat(first.get("twoFactorSetupRequired").asBoolean()).isTrue();
        String initialToken = first.get("token").asText();

        mvc.perform(get("/api/auth/2fa/status").header("Authorization", "Bearer " + initialToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/system-admin/staff").header("Authorization", "Bearer " + initialToken))
                .andExpect(status().isForbidden());

        JsonNode changed = json.readTree(mvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("currentPassword", "InitialSecurePass12!", "newPassword", "ReplacedSecurePass13!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(changed.get("mustChangePassword").asBoolean()).isFalse();
        assertThat(changed.get("twoFactorSetupRequired").asBoolean()).isTrue();
        String passwordChangedToken = changed.get("token").asText();

        mvc.perform(get("/api/auth/2fa/status").header("Authorization", "Bearer " + passwordChangedToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/system-admin/staff").header("Authorization", "Bearer " + passwordChangedToken))
                .andExpect(status().isForbidden());

        String setupBody = mvc.perform(post("/api/auth/2fa/setup").header("Authorization", "Bearer " + passwordChangedToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String secret = json.readTree(setupBody).get("secret").asText();
        String currentCode = TotpService.codeAt(secret, System.currentTimeMillis() / 1000 / 30);
        JsonNode completed = json.readTree(mvc.perform(post("/api/auth/2fa/confirm")
                        .header("Authorization", "Bearer " + passwordChangedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", currentCode))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(completed.get("twoFactorSetupRequired").asBoolean()).isFalse();
        mvc.perform(get("/api/system-admin/staff").header("Authorization", "Bearer " + completed.get("token").asText()))
                .andExpect(status().isOk());
    }
}
