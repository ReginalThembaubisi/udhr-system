package com.udhr.ux;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A patient whose diagnosis is saved stays on the doctor's queue until they
 * are sent to the pharmacy or discharged. Found in end-to-end testing: saving a
 * diagnosis moved the visit to DIAGNOSED and the patient vanished from the
 * queue before anything was prescribed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DoctorQueueTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private VisitRepository visitRepository;

    @Test
    void aDiagnosedPatientStaysOnTheDoctorsQueue() throws Exception {
        Staff doctor = staffRepository.findByStaffNumber("DOC001").orElseThrow();
        Patient patient = new Patient();
        patient.setFirstName("Queue");
        patient.setLastName("Diagnosedtest");
        patient.setPassportNumber("PQUEUE01");
        patient.setDateOfBirth(LocalDate.of(1980, 1, 1));
        patient.setGender("MALE");
        patient.setMrn("MRN-QUEUE-000001");
        patient = patientRepository.save(patient);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStaff(doctor);
        visit.setFacility(doctor.getFacility());
        visit.setVisitDate(LocalDateTime.now());
        visit.setReason("Queue test");
        visit.setStatus("DIAGNOSED");
        visit = visitRepository.save(visit);

        String token = login();
        JsonNode queue = json.readTree(mvc.perform(get("/api/visits/queue/VITALS_DONE,DIAGNOSED").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Long visitId = visit.getId();
        assertThat(queue.findValues("id").stream().map(JsonNode::asLong)).contains(visitId);

        // One status still works as before.
        JsonNode waiting = json.readTree(mvc.perform(get("/api/visits/queue/VITALS_DONE").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode v : waiting) {
            assertThat(v.get("status").asText()).isEqualTo("VITALS_DONE");
        }
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.4.0.1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", "DOC001", "password", "Doctor@123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
