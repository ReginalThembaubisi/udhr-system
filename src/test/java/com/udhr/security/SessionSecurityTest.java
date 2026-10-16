package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Staff;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.StaffRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sign-in, refresh and sign-out (fix list P1-2 to P1-7). Each test that makes
 * failed sign-ins uses its own address and account, so the lockouts it causes
 * don't leak into other tests sharing this application context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionSecurityTest {

    private static final String COOKIE = "udhr_refresh";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private StaffRepository staffRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    // ---- P1-6: one message for every wrong detail -------------------------------

    @Test
    void unknownStaffNumberAndWrongPasswordGetTheSameAnswer() throws Exception {
        String unknown = staffLogin("NOSUCH77", "whatever1", "10.0.6.1").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String wrongPassword = staffLogin("DOC001", "not-the-password", "10.0.6.1").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(unknown).isEqualTo(wrongPassword).isEqualTo("Invalid staff number or password");
    }

    // ---- P1-5: lockouts ------------------------------------------------------------

    @Test
    void fiveWrongPasswordsLockTheAccountEvenAgainstTheRightPassword() throws Exception {
        newStaff("NUR901", "RightPass1!");
        for (int i = 0; i < 5; i++) {
            staffLogin("NUR901", "WrongPass1!", "10.0.5." + i).andExpect(status().isUnauthorized());
        }
        String locked = staffLogin("NUR901", "RightPass1!", "10.0.5.99").andExpect(status().isTooManyRequests())
                .andReturn().getResponse().getContentAsString();
        assertThat(locked).contains("Too many failed sign-in attempts");
        assertThat(auditLogRepository.findAllByOrderByCreatedAtDesc())
                .anyMatch(log -> "LOGIN_LOCKED".equals(log.getAction()) && log.getDescription().contains("NUR901"));
    }

    @Test
    void unknownStaffNumbersLockLikeRealOnes() throws Exception {
        for (int i = 0; i < 5; i++) {
            staffLogin("GHOST42", "guess" + i, "10.0.4." + i).andExpect(status().isUnauthorized());
        }
        staffLogin("GHOST42", "guess", "10.0.4.99").andExpect(status().isTooManyRequests());
    }

    @Test
    void twentyFailuresFromOneAddressLockThatAddressOnly() throws Exception {
        for (int i = 0; i < 20; i++) {
            staffLogin("SPRAY" + i, "guess", "10.0.3.3").andExpect(status().isUnauthorized());
        }
        staffLogin("DOC001", "Doctor@123", "10.0.3.3").andExpect(status().isTooManyRequests());
        staffLogin("DOC001", "Doctor@123", "10.0.3.4").andExpect(status().isOk());
    }

    // ---- P1-3: refresh token only in an HttpOnly cookie ---------------------------

    @Test
    void refreshTokenIsAnHttpOnlyCookieAndNeverInTheBody() throws Exception {
        MockHttpServletResponse response = staffLogin("DOC001", "Doctor@123", "10.0.2.1")
                .andExpect(status().isOk()).andReturn().getResponse();

        assertThat(json.readTree(response.getContentAsString()).has("refreshToken")).isFalse();
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).startsWith(COOKIE + "=").contains("HttpOnly").contains("Secure")
                .contains("SameSite=Strict").contains("Path=/api/auth");
    }

    @Test
    void theCookieBuysANewAccessTokenOnceAndRotates() throws Exception {
        String first = refreshCookie(staffLogin("DOC001", "Doctor@123", "10.0.2.2").andReturn().getResponse());

        MockHttpServletResponse refreshed = refresh(first).andExpect(status().isOk()).andReturn().getResponse();
        JsonNode session = json.readTree(refreshed.getContentAsString());
        assertThat(session.get("role").asText()).isEqualTo("DOCTOR");
        mvc.perform(get("/api/visits/today").header("Authorization", "Bearer " + session.get("token").asText()))
                .andExpect(status().isOk());

        // The old cookie is used up. Presented again straight away (another tab
        // racing this one), it is refused without ending the newer session.
        refresh(first).andExpect(status().isUnauthorized());
        refresh(refreshCookie(refreshed)).andExpect(status().isOk());
    }

    @Test
    void patientsCanRefreshToo() throws Exception {
        MockHttpServletResponse login = mvc.perform(post("/api/auth/patient/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idNumber", "9001015000083", "dateOfBirth", "1990-01-01", "pin", "246810"))))
                .andExpect(status().isOk()).andReturn().getResponse();

        JsonNode session = json.readTree(refresh(refreshCookie(login)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(session.get("role").asText()).isEqualTo("PATIENT");
        assertThat(session.get("idNumber").asText()).isEqualTo("9001015000083");
    }

    @Test
    void logoutRevokesTheCookieAndTellsTheBrowserToDeleteIt() throws Exception {
        MockHttpServletResponse login = staffLogin("NUR001", "Nurse@123", "10.0.2.3").andReturn().getResponse();
        String cookie = refreshCookie(login);
        String accessToken = json.readTree(login.getContentAsString()).get("token").asText();

        String cleared = mvc.perform(post("/api/auth/logout").param("reason", "idle")
                        .header("Authorization", "Bearer " + accessToken).cookie(new Cookie(COOKIE, cookie)))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("Set-Cookie");
        assertThat(cleared).contains("Max-Age=0");

        refresh(cookie).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/visits/today").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
        assertThat(auditLogRepository.findAllByOrderByCreatedAtDesc())
                .anyMatch(log -> "LOGOUT".equals(log.getAction()) && log.getDescription().contains("without activity"));
    }

    // ---- P1-2: an expired session is 401, a wrong role is 403 ---------------------

    @Test
    void missingOrBrokenTokenIs401AndWrongRoleIs403() throws Exception {
        mvc.perform(get("/api/visits/today")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/visits/today").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());

        String admin = json.readTree(staffLogin("ADMIN001", "Admin@123", "10.0.2.4").andReturn().getResponse()
                .getContentAsString()).get("token").asText();
        mvc.perform(get("/api/visits/today").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
    }

    // ---- P1-7: browser security headers -------------------------------------------

    @Test
    void securityHeadersAreOnEveryResponse() throws Exception {
        for (String path : new String[] {"/", "/api/announcements/public"}) {
            mvc.perform(get(path))
                    .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("script-src 'self'")))
                    .andExpect(header().string("Strict-Transport-Security", org.hamcrest.Matchers.containsString("max-age=")))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
        }
    }

    // ---- helpers ------------------------------------------------------------------

    private ResultActions staffLogin(String staffNumber, String password, String address) throws Exception {
        return mvc.perform(post("/api/auth/login").header("X-Forwarded-For", address)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))));
    }

    private ResultActions refresh(String cookie) throws Exception {
        return mvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, cookie)));
    }

    private static String refreshCookie(MockHttpServletResponse response) {
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).startsWith(COOKIE + "=");
        return setCookie.substring(COOKIE.length() + 1, setCookie.indexOf(';'));
    }

    private void newStaff(String staffNumber, String password) {
        Staff staff = new Staff();
        staff.setStaffNumber(staffNumber);
        staff.setFirstName("Test");
        staff.setLastName("Nurse");
        staff.setRole("NURSE");
        staff.setFacility(staffRepository.findByStaffNumber("NUR001").orElseThrow().getFacility());
        staff.setEmail(staffNumber.toLowerCase() + "@test.local");
        staff.setPassword(passwordEncoder.encode(password));
        staff.setActive(true);
        staff.setMustChangePassword(false);
        staffRepository.save(staff);
    }
}
