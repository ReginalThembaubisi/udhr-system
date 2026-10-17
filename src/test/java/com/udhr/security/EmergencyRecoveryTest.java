package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.config.EmergencyRecovery;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recovering the system admin from the server settings, once. */
@SpringBootTest(properties = {
        "udhr.sysadmin-recovery=SYSADMIN001",
        "udhr.sysadmin-initial-password=Recovery-Temp-2026",
        "spring.datasource.url=jdbc:h2:mem:udhr_recovery;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class EmergencyRecoveryTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private EmergencyRecovery emergencyRecovery;
    @Autowired private StaffRepository staffRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Test
    void recoversTheAccountOnceWithATemporaryPassword() throws Exception {
        JsonNode session = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", "SYSADMIN001", "password", "Recovery-Temp-2026"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(session.get("mustChangePassword").asBoolean()).isTrue();

        // A later restart with the same settings changes nothing.
        String hashAfterFirst = staffRepository.findByStaffNumber("SYSADMIN001").orElseThrow().getPassword();
        emergencyRecovery.run();
        assertThat(staffRepository.findByStaffNumber("SYSADMIN001").orElseThrow().getPassword()).isEqualTo(hashAfterFirst);
        assertThat(auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(log -> "EMERGENCY_RECOVERY".equals(log.getAction())).count()).isEqualTo(1);
    }
}
