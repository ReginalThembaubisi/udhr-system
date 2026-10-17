package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** With the production default, a system admin must set up two-step sign-in before doing anything. */
@SpringBootTest(properties = {
        "udhr.sysadmin-2fa=required",
        "spring.datasource.url=jdbc:h2:mem:udhr_sysadmin_2fa;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class SysadminTwoFactorRequiredTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void systemAdminMustSetUpTwoStepFirstButOtherStaffNeedNot() throws Exception {
        JsonNode admin = login("SYSADMIN001", "SysAdmin@123");
        assertThat(admin.get("twoFactorSetupRequired").asBoolean()).isTrue();
        String token = admin.get("token").asText();
        mvc.perform(get("/api/system-admin/staff").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        String status = mvc.perform(get("/api/auth/2fa/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(status).get("required").asBoolean()).isTrue();

        assertThat(login("DOC001", "Doctor@123").get("twoFactorSetupRequired").asBoolean()).isFalse();
    }

    private JsonNode login(String staffNumber, String password) throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
}
