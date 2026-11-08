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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With no email service connected (the demo), alert emails go to the demo inbox instead. */
@SpringBootTest
@AutoConfigureMockMvc
class DemoInboxTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void theTestEmailLandsInTheDemoInbox() throws Exception {
        String admin = login("SYSADMIN001", "SysAdmin@123");
        mvc.perform(get("/api/system-admin/alert-email").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(false));
        mvc.perform(post("/api/system-admin/alert-email/test").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        JsonNode inbox = json.readTree(mvc.perform(get("/api/system-admin/demo-inbox").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(inbox.size()).isGreaterThan(0);
        assertThat(inbox.get(0).get("subject").asText()).isEqualTo("UDHR security alert: test email");

        // Only the system admin can read it.
        mvc.perform(get("/api/system-admin/demo-inbox").header("Authorization", "Bearer " + login("DOC001", "Doctor@123")))
                .andExpect(status().isForbidden());
    }

    private String login(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.2.9.3").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
