package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.audit.AccessReasonInterceptor;
import com.udhr.audit.AuditFloodGuard;
import com.udhr.model.AuditLog;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.VisitRepository;
import com.udhr.service.SecurityAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Protecting patient data if a doctor's or nurse's account is misused:
 * a reason is needed to open a patient outside today's list, the system admin
 * is warned about unusual activity, and signed-out requests can't flood the
 * audit trail.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecordAccessSafeguardsTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private SecurityAlertService securityAlertService;
    @Autowired private AuditFloodGuard auditFloodGuard;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private VisitRepository visitRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void openingAPatientOutsideTodaysListNeedsAReason() throws Exception {
        Patient stranger = patient("PREASON01", "MRN-REASON-000001");
        stranger.setIdNumber("7712125000188");
        stranger = patientRepository.save(stranger);
        String doctor = login("DOC001", "Doctor@123");

        JsonNode refused = json.readTree(mvc.perform(get("/api/patients/PREASON01").header("Authorization", "Bearer " + doctor))
                .andExpect(status().is(428)).andReturn().getResponse().getContentAsString());
        assertThat(refused.get("reasonRequired").asBoolean()).isTrue();
        // Every read of that patient is held back, not only the first look-up:
        // the doctor's search uses /record with the SA ID number.
        mvc.perform(get("/api/vitals/patient/" + stranger.getId()).header("Authorization", "Bearer " + doctor))
                .andExpect(status().is(428));
        mvc.perform(get("/api/patients/7712125000188/record").header("Authorization", "Bearer " + doctor))
                .andExpect(status().is(428));
        mvc.perform(post("/api/clinical-alerts/patient/" + stranger.getId() + "/evaluate").header("Authorization", "Bearer " + doctor))
                .andExpect(status().is(428));

        String reason = URLEncoder.encode("Emergency: brought in by ambulance", StandardCharsets.UTF_8);
        mvc.perform(get("/api/patients/PREASON01").header("Authorization", "Bearer " + doctor)
                        .header(AccessReasonInterceptor.HEADER, reason))
                .andExpect(status().isOk());
        // The reason covers that patient for a while; no prompt for each screen.
        mvc.perform(get("/api/vitals/patient/" + stranger.getId()).header("Authorization", "Bearer " + doctor))
                .andExpect(status().isOk());

        List<AuditLog> entries = auditLogRepository.findByPatientIdOrderByCreatedAtDesc(stranger.getId()).stream()
                .filter(l -> AccessReasonInterceptor.ACTION.equals(l.getAction())).toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getDescription()).contains("Emergency: brought in by ambulance");

        // Another member of staff still has to give their own reason.
        String nurse = login("NUR001", "Nurse@123");
        mvc.perform(get("/api/patients/PREASON01").header("Authorization", "Bearer " + nurse))
                .andExpect(status().is(428));
    }

    @Test
    void aPatientOnTodaysListOpensWithoutAReason() throws Exception {
        // The seeded demo patient has open visits at the doctor's clinic.
        String doctor = login("DOC001", "Doctor@123");
        mvc.perform(get("/api/patients/9001015000083").header("Authorization", "Bearer " + doctor))
                .andExpect(status().isOk());
    }

    @Test
    void aVisitLeftOpenForWeeksDoesNotCountAsToday() throws Exception {
        // Found on the live site: visits are often never closed, and a patient
        // with one left open since September skipped the prompt for good.
        Staff doctor = staffRepository.findByStaffNumber("DOC001").orElseThrow();
        Patient stale = patient("PSTALE01", "MRN-STALE-000001");
        openVisit(stale, doctor, LocalDateTime.now().minusDays(10));
        Patient recent = patient("PRECENT01", "MRN-RECENT-000001");
        openVisit(recent, doctor, LocalDateTime.now().minusDays(1));

        String token = login("DOC001", "Doctor@123");
        mvc.perform(get("/api/patients/PSTALE01").header("Authorization", "Bearer " + token))
                .andExpect(status().is(428));
        mvc.perform(get("/api/patients/PRECENT01").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void openVisit(Patient patient, Staff staff, LocalDateTime when) {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStaff(staff);
        visit.setFacility(staff.getFacility());
        visit.setVisitDate(when);
        visit.setReason("Left open");
        visit.setStatus("VITALS_DONE");
        visit = visitRepository.save(visit);
        // A visit stamps "now" when saved and its date can't be changed through
        // JPA (updatable = false), so the test sets it in the table.
        jdbc.update("update visits set visit_date = ? where id = ?", when, visit.getId());
    }

    @Test
    void theSystemAdminIsWarnedAboutUnusualActivity() throws Exception {
        Staff doctor = staffRepository.findByStaffNumber("DOC001").orElseThrow();
        Staff watched = new Staff();
        watched.setStaffNumber("DOCALERT1");
        watched.setFirstName("Alert");
        watched.setLastName("Testdoctor");
        watched.setRole("DOCTOR");
        watched.setEmail("alert.test@example.org");
        watched.setFacility(doctor.getFacility());
        watched.setPassword(passwordEncoder.encode("NotUsed@12345"));
        watched.setActive(true);
        watched = staffRepository.save(watched);

        LocalDateTime start = LocalDateTime.now().minusMinutes(50);
        for (int i = 0; i < 16; i++) {
            Patient p = patient("PALERT" + i, "MRN-ALERT-0000" + String.format("%02d", i));
            log(watched, p, "VIEW_PATIENT_DATA", "viewed demographics", null, start.plusMinutes(i));
        }
        log(watched, null, "LOGIN_SUCCESS", "signed in", "10.10.0.1", LocalDateTime.now().minusDays(3));
        log(watched, null, "LOGIN_SUCCESS", "signed in", "203.0.113.9", LocalDateTime.now().minusMinutes(5));
        log(watched, null, "LOGIN_LOCKED", "Sign-in locked for 15 minutes", "203.0.113.9", LocalDateTime.now().minusMinutes(4));

        Long id = watched.getId();
        List<SecurityAlertService.Alert> mine = securityAlertService.alerts(7).stream()
                .filter(a -> id.equals(a.staffId())).toList();
        assertThat(mine).extracting(SecurityAlertService.Alert::type).contains("MANY_RECORDS", "NEW_PLACE", "LOCKED");
        assertThat(mine.stream().filter(a -> a.type().equals("MANY_RECORDS")).findFirst().orElseThrow().severity()).isEqualTo("HIGH");
        assertThat(mine.stream().filter(a -> a.type().equals("NEW_PLACE")).findFirst().orElseThrow().title()).contains("203.0.113.9");

        // Only the system admin sees them.
        String sysAdmin = login("SYSADMIN001", "SysAdmin@123");
        mvc.perform(get("/api/system-admin/security-alerts").header("Authorization", "Bearer " + sysAdmin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/system-admin/security-alerts").header("Authorization", "Bearer " + login("DOC001", "Doctor@123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedOutRequestsCantFloodTheAuditTrail() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.77");
        for (int i = 0; i < 5; i++) {
            assertThat(auditFloodGuard.check(request)).isEqualTo(AuditFloodGuard.Decision.RECORD);
        }
        assertThat(auditFloodGuard.check(request)).isEqualTo(AuditFloodGuard.Decision.RECORD_LIMIT_REACHED);
        assertThat(auditFloodGuard.check(request)).isEqualTo(AuditFloodGuard.Decision.SKIP);

        MockHttpServletRequest other = new MockHttpServletRequest();
        other.addHeader("X-Forwarded-For", "198.51.100.78");
        assertThat(auditFloodGuard.check(other)).isEqualTo(AuditFloodGuard.Decision.RECORD);
    }

    private Patient patient(String passport, String mrn) {
        Patient p = new Patient();
        p.setFirstName("Safeguard");
        p.setLastName(passport);
        p.setPassportNumber(passport);
        p.setDateOfBirth(LocalDate.of(1990, 5, 5));
        p.setGender("FEMALE");
        p.setMrn(mrn);
        return patientRepository.save(p);
    }

    private void log(Staff staff, Patient patient, String action, String description, String ip, LocalDateTime at) {
        AuditLog entry = new AuditLog();
        entry.setStaff(staff);
        entry.setPatient(patient);
        entry.setAction(action);
        entry.setDescription(description);
        entry.setIpAddress(ip);
        entry.setCreatedAt(at);
        auditLogRepository.save(entry);
    }

    private String login(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.2.9.1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
