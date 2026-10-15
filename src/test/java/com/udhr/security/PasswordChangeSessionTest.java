package com.udhr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Staff;
import com.udhr.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fix list P1-1: the token handed back after a password change used to be
 * rejected on its very first use, so every new account (which must change its
 * temporary password) was thrown out straight after setting one.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordChangeSessionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StaffRepository staffRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Test
    void tokenFromPasswordChangeWorksImmediately_andOldTokenDoesNot() throws Exception {
        Staff nurse = new Staff();
        nurse.setStaffNumber("NUR555");
        nurse.setFirstName("New");
        nurse.setLastName("Nurse");
        nurse.setRole("NURSE");
        nurse.setFacility(staffRepository.findByStaffNumber("NUR001").orElseThrow().getFacility());
        nurse.setEmail("nur555@test.local");
        nurse.setPassword(passwordEncoder.encode("TempPass1!"));
        nurse.setActive(true);
        nurse.setMustChangePassword(true);
        staffRepository.save(nurse);

        String tempToken = token(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", "NUR555", "password", "TempPass1!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        String newToken = token(mvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + tempToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("currentPassword", "TempPass1!", "newPassword", "NewPass12!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/visits/today").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());

        // Ending every other session still works: the pre-change token is dead.
        Thread.sleep(1100);
        mvc.perform(get("/api/visits/today").header("Authorization", "Bearer " + tempToken))
                .andExpect(status().is4xxClientError());
    }

    private String token(String body) throws Exception {
        return json.readTree(body).get("token").asText();
    }
}
