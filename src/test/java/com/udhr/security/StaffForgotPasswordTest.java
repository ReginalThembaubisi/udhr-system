package com.udhr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.DemoEmail;
import com.udhr.model.Staff;
import com.udhr.repository.DemoEmailRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.service.StaffPasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A member of staff who forgot their password gets a new one with a code emailed to their work email. */
@SpringBootTest
@AutoConfigureMockMvc
class StaffForgotPasswordTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StaffRepository staffRepository;
    @Autowired private DemoEmailRepository demoInbox;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Test
    void anEmailedCodeLetsTheDoctorChooseANewPassword() throws Exception {
        staff("DOCFORGOT1", "forgot.one@example.org", true);
        forgot("DOCFORGOT1", "10.8.0.1").andExpect(jsonPath("$.message").value(StaffPasswordResetService.SENT_MESSAGE));
        String code = codeIn(latestEmailTo("forgot.one@example.org").getBody());

        reset("DOCFORGOT1", code.equals("000000") ? "111111" : "000000", "BrandNew#2026").andExpect(status().isBadRequest());
        reset("DOCFORGOT1", code, "short").andExpect(status().isBadRequest());
        reset("DOCFORGOT1", code, "BrandNew#2026").andExpect(status().isOk());
        login("DOCFORGOT1", "BrandNew#2026").andExpect(status().isOk());
        login("DOCFORGOT1", "OldPassword#1").andExpect(status().isUnauthorized());
        reset("DOCFORGOT1", code, "Another#2026x").andExpect(status().isBadRequest()); // used once only
    }

    @Test
    void unknownAndDeactivatedAccountsGetTheSameAnswerAndNoEmail() throws Exception {
        staff("DOCFORGOT2", "forgot.two@example.org", false);
        long before = demoInbox.count();
        forgot("NOSUCHSTAFF", "10.8.0.2").andExpect(jsonPath("$.message").value(StaffPasswordResetService.SENT_MESSAGE));
        forgot("DOCFORGOT2", "10.8.0.2").andExpect(jsonPath("$.message").value(StaffPasswordResetService.SENT_MESSAGE));
        assertThat(demoInbox.count()).isEqualTo(before);
    }

    @Test
    void fiveWrongCodesCancelTheCode() throws Exception {
        staff("DOCFORGOT3", "forgot.three@example.org", true);
        forgot("DOCFORGOT3", "10.8.0.3");
        String code = codeIn(latestEmailTo("forgot.three@example.org").getBody());
        String wrong = code.equals("123450") ? "123451" : "123450";
        for (int i = 0; i < 5; i++) {
            reset("DOCFORGOT3", wrong, "BrandNew#2026").andExpect(status().isBadRequest());
        }
        reset("DOCFORGOT3", code, "BrandNew#2026").andExpect(status().isBadRequest());
    }

    private void staff(String number, String email, boolean active) {
        Staff doctor = staffRepository.findByStaffNumber("DOC001").orElseThrow();
        Staff s = new Staff();
        s.setStaffNumber(number);
        s.setFirstName("Forgot");
        s.setLastName("Password");
        s.setRole("DOCTOR");
        s.setEmail(email);
        s.setFacility(doctor.getFacility());
        s.setPassword(passwordEncoder.encode("OldPassword#1"));
        s.setMustChangePassword(false);
        s.setActive(active);
        staffRepository.save(s);
    }

    private DemoEmail latestEmailTo(String address) {
        return demoInbox.findTop20ByOrderBySentAtDesc().stream()
                .filter(e -> address.equals(e.getRecipients())).findFirst().orElseThrow();
    }

    private static String codeIn(String text) {
        Matcher m = Pattern.compile("(\\d{6})").matcher(text);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private ResultActions forgot(String number, String address) throws Exception {
        return mvc.perform(post("/api/auth/forgot-password").header("X-Forwarded-For", address)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("staffNumber", number))))
                .andExpect(status().isOk());
    }

    private ResultActions reset(String number, String code, String password) throws Exception {
        return mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("staffNumber", number, "code", code, "newPassword", password))));
    }

    private ResultActions login(String number, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.8.0.9").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("staffNumber", number, "password", password))));
    }
}
