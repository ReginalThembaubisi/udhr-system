package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.DemoEmail;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.repository.DemoEmailRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.service.TotpService;
import com.udhr.service.TrustedDeviceService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A new device needs an extra code (authenticator app for staff, a text
 * message for patients); a device that gave it is remembered for 30 days,
 * until the account's sessions are ended.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "udhr.patient-new-device-check=on")
class NewDeviceSignInTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StaffRepository staffRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DemoEmailRepository demoInbox;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Test
    void aRememberedDeviceSkipsTheAuthenticatorCodeANewOneDoesNot() throws Exception {
        newStaff("DOC801", "Doctor@801");
        String token = json.readTree(login("DOC801", "Doctor@801", null).getContentAsString()).get("token").asText();
        String secret = json.readTree(mvc.perform(post("/api/auth/2fa/setup").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("secret").asText();
        long now = System.currentTimeMillis() / 1000 / 30;
        // Setting up the app remembers the device it was set up on.
        MockHttpServletResponse confirmed = mvc.perform(post("/api/auth/2fa/confirm").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("code", TotpService.codeAt(secret, now)))))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie device = deviceCookie(confirmed);
        assertThat(device).isNotNull();
        assertThat(device.isHttpOnly()).isTrue();

        // Same device: password alone.
        JsonNode remembered = json.readTree(login("DOC801", "Doctor@801", device).getContentAsString());
        assertThat(remembered.get("token").isNull()).isFalse();
        // A new device (no cookie, or someone else's): the code is asked for.
        assertThat(json.readTree(login("DOC801", "Doctor@801", null).getContentAsString()).get("twoFactorRequired").asBoolean()).isTrue();
        assertThat(json.readTree(login("DOC801", "Doctor@801", new Cookie(TrustedDeviceService.COOKIE, "x".repeat(43)))
                .getContentAsString()).get("twoFactorRequired").asBoolean()).isTrue();

        // "Forget all devices": the old device needs the code again.
        mvc.perform(post("/api/auth/devices/forget").header("Authorization", "Bearer " + remembered.get("token").asText()))
                .andExpect(status().isOk());
        assertThat(json.readTree(login("DOC801", "Doctor@801", device).getContentAsString()).get("twoFactorRequired").asBoolean()).isTrue();
    }

    @Test
    void aPatientOnANewDeviceNeedsTheCodeTextedToTheirPhone() throws Exception {
        Patient p = new Patient();
        p.setFirstName("Device");
        p.setLastName("Checkpatient");
        p.setIdNumber("7905055111086");
        p.setDateOfBirth(LocalDate.of(1979, 5, 5));
        p.setGender("MALE");
        p.setMrn("MRN-DEVICE-000001");
        p.setContactNumber("0719998877");
        p.setPortalPinHash(passwordEncoder.encode("583920"));
        patientRepository.save(p);

        // Right PIN on a new device: no session yet.
        JsonNode challenge = json.readTree(patientLogin(null).getContentAsString());
        assertThat(challenge.get("deviceCodeRequired").asBoolean()).isTrue();
        assertThat(challenge.get("token").isNull()).isTrue();
        String text = demoInbox.findTop20ByOrderBySentAtDesc().stream()
                .filter(e -> "SMS".equals(e.getKind()) && e.getRecipients().endsWith("8877")).findFirst()
                .map(DemoEmail::getBody).orElseThrow();
        Matcher m = Pattern.compile("(\\d{6})").matcher(text);
        assertThat(m.find()).isTrue();
        // The challenge itself is not a sign-in.
        mvc.perform(post("/api/patient/me/change-pin").header("Authorization", "Bearer " + challenge.get("challengeToken").asText()))
                .andExpect(status().is4xxClientError());

        MockHttpServletResponse verified = mvc.perform(post("/api/auth/patient/login/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("challengeToken", challenge.get("challengeToken").asText(), "code", m.group(1)))))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(json.readTree(verified.getContentAsString()).get("token").isNull()).isFalse();
        Cookie device = deviceCookie(verified);

        // That device is remembered: PIN alone next time.
        assertThat(json.readTree(patientLogin(device).getContentAsString()).get("token").isNull()).isFalse();
    }

    private MockHttpServletResponse patientLogin(Cookie device) throws Exception {
        var request = post("/api/auth/patient/login").header("X-Forwarded-For", "10.9.0.5").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idNumber", "7905055111086", "dateOfBirth", "1979-05-05", "pin", "583920")));
        if (device != null) request.cookie(device);
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
    }

    private MockHttpServletResponse login(String number, String password, Cookie device) throws Exception {
        var request = post("/api/auth/login").header("X-Forwarded-For", "10.9.0.1").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("staffNumber", number, "password", password)));
        if (device != null) request.cookie(device);
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
    }

    private static Cookie deviceCookie(MockHttpServletResponse response) {
        return response.getCookie(TrustedDeviceService.COOKIE);
    }

    private void newStaff(String number, String password) {
        Staff doctor = staffRepository.findByStaffNumber("DOC001").orElseThrow();
        Staff s = new Staff();
        s.setStaffNumber(number);
        s.setFirstName("Device");
        s.setLastName("Testdoctor");
        s.setRole("DOCTOR");
        s.setEmail(number.toLowerCase() + "@example.org");
        s.setFacility(doctor.getFacility());
        s.setPassword(passwordEncoder.encode(password));
        s.setMustChangePassword(false);
        s.setActive(true);
        staffRepository.save(s);
    }
}
