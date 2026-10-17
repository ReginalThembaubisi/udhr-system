package com.udhr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/** The system can't be left without anyone able to manage it. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminSafeguardsTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StaffRepository staffRepository;

    @Test
    void aSystemAdminCannotDeactivateThemselvesOrResetTheirOwnSecondStep() throws Exception {
        String sysAdmin = login();
        Long ownId = staffRepository.findByStaffNumber("SYSADMIN001").orElseThrow().getId();

        String refused = mvc.perform(post("/api/system-admin/staff/" + ownId + "/active").header("Authorization", "Bearer " + sysAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(refused).contains("your own account");
        assertThat(staffRepository.findById(ownId).orElseThrow().getActive()).isTrue();

        mvc.perform(post("/api/system-admin/staff/" + ownId + "/reset-two-factor").header("Authorization", "Bearer " + sysAdmin))
                .andExpect(status().isBadRequest());
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.2.0.1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", "SYSADMIN001", "password", "SysAdmin@123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
