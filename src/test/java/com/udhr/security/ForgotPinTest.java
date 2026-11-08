package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.DemoEmail;
import com.udhr.model.Patient;
import com.udhr.repository.DemoEmailRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.service.PatientPinResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A patient who forgot their PIN gets a new one with a code texted to the cellphone on their record. */
@SpringBootTest
@AutoConfigureMockMvc
class ForgotPinTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DemoEmailRepository demoInbox;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Test
    void aCodeTextedToTheirPhoneLetsThePatientChooseANewPin() throws Exception {
        patient("8203035111081", LocalDate.of(1982, 3, 3), "0731112233");

        String message = forgot("8203035111081", "1982-03-03", "10.7.0.1").get("message").asText();
        assertThat(message).isEqualTo(PatientPinResetService.SENT_MESSAGE);
        DemoEmail sms = latestText("****2233");
        String code = codeIn(sms.getBody());

        // A wrong code is refused; the right one sets the PIN.
        reset("8203035111081", "1982-03-03", code.equals("000000") ? "111111" : "000000", "583920").andExpect(status().isBadRequest());
        reset("8203035111081", "1982-03-03", code, "583920").andExpect(status().isOk());
        login("8203035111081", "1982-03-03", "583920").andExpect(status().isOk());
        // The code works once.
        reset("8203035111081", "1982-03-03", code, "694031").andExpect(status().isBadRequest());
    }

    @Test
    void theAnswerIsTheSameWhetherOrNotThePatientExists() throws Exception {
        long before = demoInbox.count();
        assertThat(forgot("0000000000000", "1990-01-01", "10.7.0.2").get("message").asText())
                .isEqualTo(PatientPinResetService.SENT_MESSAGE);
        // Right ID, wrong date of birth: same answer, and no text sent.
        patient("8404045222082", LocalDate.of(1984, 4, 4), "0742223344");
        assertThat(forgot("8404045222082", "1984-05-05", "10.7.0.2").get("message").asText())
                .isEqualTo(PatientPinResetService.SENT_MESSAGE);
        assertThat(demoInbox.count()).isEqualTo(before);
    }

    @Test
    void fiveWrongCodesCancelTheCode() throws Exception {
        patient("8605055333083", LocalDate.of(1986, 5, 5), "0753334455");
        forgot("8605055333083", "1986-05-05", "10.7.0.3");
        String code = codeIn(latestText("****4455").getBody());
        String wrong = code.equals("123450") ? "123451" : "123450";
        for (int i = 0; i < 5; i++) {
            reset("8605055333083", "1986-05-05", wrong, "472910").andExpect(status().isBadRequest());
        }
        reset("8605055333083", "1986-05-05", code, "472910").andExpect(status().isBadRequest());
    }

    @Test
    void theCodeIsNeverSentBackToTheBrowser() throws Exception {
        patient("8806065444084", LocalDate.of(1988, 6, 6), "0764445566");
        String body = mvc.perform(post("/api/auth/patient/forgot-pin").header("X-Forwarded-For", "10.7.0.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idNumber", "8806065444084", "dateOfBirth", "1988-06-06"))))
                .andReturn().getResponse().getContentAsString();
        String code = codeIn(latestText("****5566").getBody());
        assertThat(body).doesNotContain(code).doesNotContain("5566");
    }

    private void patient(String id, LocalDate dob, String phone) {
        Patient p = new Patient();
        p.setFirstName("Forgot");
        p.setLastName("Pin" + id.substring(9));
        p.setIdNumber(id);
        p.setDateOfBirth(dob);
        p.setGender("FEMALE");
        p.setMrn("MRN-FORGOT-" + id.substring(7));
        p.setContactNumber(phone);
        p.setPortalPinHash(passwordEncoder.encode("905172"));
        patientRepository.save(p);
    }

    private DemoEmail latestText(String to) {
        return demoInbox.findTop20ByOrderBySentAtDesc().stream()
                .filter(e -> "SMS".equals(e.getKind()) && e.getRecipients().endsWith(to))
                .findFirst().orElseThrow();
    }

    private static String codeIn(String text) {
        Matcher m = Pattern.compile("(\\d{6})").matcher(text);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private JsonNode forgot(String id, String dob, String address) throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/patient/forgot-pin").header("X-Forwarded-For", address)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idNumber", id, "dateOfBirth", dob))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions reset(String id, String dob, String code, String pin) throws Exception {
        return mvc.perform(post("/api/auth/patient/reset-pin").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idNumber", id, "dateOfBirth", dob, "code", code, "newPin", pin))));
    }

    private org.springframework.test.web.servlet.ResultActions login(String id, String dob, String pin) throws Exception {
        return mvc.perform(post("/api/auth/patient/login").header("X-Forwarded-For", "10.7.0.9").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idNumber", id, "dateOfBirth", dob, "pin", pin))));
    }
}
