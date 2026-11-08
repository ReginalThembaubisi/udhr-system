package com.udhr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.AuditLog;
import com.udhr.model.Staff;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.service.AlertEmailSender;
import com.udhr.service.SecurityAlertMailer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** High security alerts are emailed to the system admins, once, without patient details. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAlertEmailTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private SecurityAlertMailer mailer;
    @Autowired private StaffRepository staffRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @MockBean private AlertEmailSender sender;

    @Test
    @SuppressWarnings("unchecked")
    void aLockoutIsEmailedToTheSystemAdminsOnce() {
        when(sender.configured()).thenReturn(true);
        when(sender.send(anyList(), anyString(), anyString())).thenReturn(true);

        Staff doctor = staffRepository.findByStaffNumber("DOC001").orElseThrow();
        Staff locked = new Staff();
        locked.setStaffNumber("DOCMAIL1");
        locked.setFirstName("Mail");
        locked.setLastName("Lockeddoctor");
        locked.setRole("DOCTOR");
        locked.setEmail("mail.locked@example.org");
        locked.setFacility(doctor.getFacility());
        locked.setPassword(passwordEncoder.encode("NotUsed@12345"));
        locked.setActive(true);
        locked = staffRepository.save(locked);

        AuditLog lock = new AuditLog();
        lock.setStaff(locked);
        lock.setAction("LOGIN_LOCKED");
        lock.setDescription("Sign-in locked for 15 minutes");
        lock.setIpAddress("203.0.113.50");
        lock.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        auditLogRepository.save(lock);

        mailer.sendNewAlerts();
        ArgumentCaptor<List<String>> to = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).send(to.capture(), subject.capture(), body.capture());

        int mine = -1;
        for (int i = 0; i < subject.getAllValues().size(); i++) {
            if (subject.getAllValues().get(i).contains("Mail Lockeddoctor")) mine = i;
        }
        assertThat(mine).as("an email about the locked account").isGreaterThanOrEqualTo(0);
        String sysAdminEmail = staffRepository.findByStaffNumber("SYSADMIN001").orElseThrow().getEmail();
        assertThat(to.getAllValues().get(mine)).contains(sysAdminEmail);
        assertThat(body.getAllValues().get(mine)).contains("DOCMAIL1", "End all sessions");

        // The next check doesn't send it again.
        clearInvocations(sender);
        mailer.sendNewAlerts();
        verify(sender, never()).send(anyList(), contains("Mail Lockeddoctor"), anyString());
    }

    @Test
    void noPatientDetailsInTheEmail() {
        when(sender.configured()).thenReturn(true);
        when(sender.send(anyList(), anyString(), anyString())).thenReturn(true);
        mailer.sendNewAlerts();
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeast(0)).send(anyList(), anyString(), body.capture());
        for (String text : body.getAllValues()) {
            assertThat(text).doesNotContain("MRN-").doesNotContain("Reason");
        }
    }

    @Test
    void theSystemAdminCanChangeAnEmailAndSendATest() throws Exception {
        when(sender.configured()).thenReturn(true);
        when(sender.send(anyList(), anyString(), anyString())).thenReturn(true);
        String admin = login("SYSADMIN001", "SysAdmin@123");
        Staff nurse = staffRepository.findByStaffNumber("NUR001").orElseThrow();
        String before = nurse.getEmail();

        mvc.perform(post("/api/system-admin/staff/" + nurse.getId() + "/email").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/system-admin/staff/" + nurse.getId() + "/email").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"New.Nurse@Example.org\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new.nurse@example.org"));
        // Put it back for the other tests.
        mvc.perform(post("/api/system-admin/staff/" + nurse.getId() + "/email").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email", before))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/system-admin/alert-email").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(true));
        mvc.perform(post("/api/system-admin/alert-email/test").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        verify(sender).send(anyList(), eq("UDHR security alert: test email"), anyString());

        // Nobody else can.
        String doctor = login("DOC001", "Doctor@123");
        mvc.perform(post("/api/system-admin/staff/" + nurse.getId() + "/email").header("Authorization", "Bearer " + doctor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"x@example.org\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/system-admin/alert-email/test").header("Authorization", "Bearer " + doctor))
                .andExpect(status().isForbidden());
    }

    private String login(String staffNumber, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.2.9.2").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
