package com.udhr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.health.DatabaseHealth;
import com.udhr.health.SystemStatusController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rubric: "what happens if the database is empty or moved?" */
@SpringBootTest
@AutoConfigureMockMvc
class DatabaseDownTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private DatabaseHealth health;

    @Test
    void whenTheDatabaseIsUnreachableEveryoneGetsOneClearMessage() throws Exception {
        when(health.isUp()).thenReturn(false);
        when(health.checkNow()).thenReturn(false);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", "DOC001", "password", "Doctor@123"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.databaseUnavailable").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("database can't be reached")));
        mvc.perform(get("/api/visits/today")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/system-status"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.database").value("unavailable"));
        // Pages outside /api aren't held back, so the message can be shown.
        // (No built frontend in the test run, so "/" may be 404, but never 503.)
        mvc.perform(get("/")).andExpect(result ->
                org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(503));
    }

    @Test
    void theSystemAdminSeesWhichDatabaseAndWhereItIsConfigured() throws Exception {
        when(health.isUp()).thenReturn(true);
        when(health.checkNow()).thenReturn(true);
        when(health.product()).thenReturn("H2 test");
        String token = json.readTree(mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.12.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", "SYSADMIN001", "password", "SysAdmin@123"))))
                .andReturn().getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/system-admin/system-status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value("connected"))
                .andExpect(jsonPath("$.configuredIn").value(org.hamcrest.Matchers.containsString("SPRING_DATASOURCE_URL")))
                .andExpect(jsonPath("$.records.patients").isNumber())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
        mvc.perform(get("/api/system-status")).andExpect(jsonPath("$.database").value("connected"));
    }

    @Test
    void theDatabasePasswordIsNeverShown() {
        org.assertj.core.api.Assertions.assertThat(
                SystemStatusControllerAccess.mask("jdbc:postgresql://admin:s3cret@db.example.org:5432/udhr?sslmode=require"))
                .isEqualTo("jdbc:postgresql://db.example.org:5432/udhr");
    }

    static class SystemStatusControllerAccess {
        static String mask(String url) {
            try {
                var m = SystemStatusController.class.getDeclaredMethod("maskedUrl", String.class);
                m.setAccessible(true);
                return (String) m.invoke(null, url);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
