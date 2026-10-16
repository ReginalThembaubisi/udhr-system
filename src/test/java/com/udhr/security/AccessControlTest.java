package com.udhr.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Facility;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Staff;
import com.udhr.repository.FacilityRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.PrescriptionRepository;
import com.udhr.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who may do what, checked through the real HTTP layer against the seeded demo
 * data (DataSeeder). Each test is one of the access-control holes found in the
 * October 2026 audit (docs/UDHR-Fix-List.md, workstream A), so it fails again
 * if the hole is ever reopened.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccessControlTest {

    private static final String SEEDED_PATIENT_ID = "9001015000083";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private FacilityRepository facilityRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    private Facility homeFacility;

    @BeforeEach
    void otherFacilityPharmacist() {
        homeFacility = staffRepository.findByStaffNumber("PHARM001").orElseThrow().getFacility();
        if (staffRepository.findByStaffNumber("PHARM900").isPresent()) {
            return;
        }
        Facility other = new Facility();
        other.setName("Other Clinic");
        other.setType("CLINIC");
        other.setProvince("Gauteng");
        other.setAddress("1 Test Street");
        other = facilityRepository.save(other);

        Staff pharmacist = new Staff();
        pharmacist.setStaffNumber("PHARM900");
        pharmacist.setFirstName("Other");
        pharmacist.setLastName("Pharmacist");
        pharmacist.setRole("PHARMACIST");
        pharmacist.setFacility(other);
        pharmacist.setEmail("pharm900@test.local");
        pharmacist.setPassword(passwordEncoder.encode("Pharmacist@900"));
        pharmacist.setActive(true);
        pharmacist.setMustChangePassword(false);
        staffRepository.save(pharmacist);
    }

    // ---- P0-1: only the system administrator manages accounts --------------------

    @Test
    void frontDeskCannotCreateAnAccount() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        mvc.perform(post("/api/staff").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAccount("EVIL01", "SYSTEM_ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(staffRepository.findByStaffNumber("EVIL01")).isEmpty();
    }

    @Test
    void frontDeskCannotDeactivateAnAccount() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        Long sysAdminId = staffRepository.findByStaffNumber("SYSADMIN001").orElseThrow().getId();
        mvc.perform(put("/api/staff/" + sysAdminId + "/deactivate").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        assertThat(staffRepository.findById(sysAdminId).orElseThrow().getActive()).isTrue();
    }

    @Test
    void systemAdminCanCreateAnAccount() throws Exception {
        String sysAdmin = login("SYSADMIN001", "SysAdmin@123");
        mvc.perform(post("/api/staff").header("Authorization", bearer(sysAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAccount("NUR777", "NURSE")))
                .andExpect(status().isCreated());
        assertThat(staffRepository.findByStaffNumber("NUR777")).isPresent();
    }

    // ---- P0-6: pharmacy actions stay inside the pharmacist's own facility --------

    @Test
    void pharmacistAtAnotherFacilityCannotAdjustStock() throws Exception {
        String home = login("PHARM001", "Pharmacist@123");
        MvcResult created = mvc.perform(post("/api/stock").header("Authorization", bearer(home))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "medicationName", "Test Paracetamol " + System.nanoTime(),
                                "unit", "tablets", "quantityOnHand", 100, "reorderLevel", 10))))
                .andExpect(status().isCreated())
                .andReturn();
        long itemId = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String adjust = json.writeValueAsString(Map.of("stockItemId", itemId, "quantityChange", -5));

        String other = login("PHARM900", "Pharmacist@900");
        mvc.perform(post("/api/stock/adjust").header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON).content(adjust))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/stock/" + itemId + "/history").header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/stock/adjust").header("Authorization", bearer(home))
                        .contentType(MediaType.APPLICATION_JSON).content(adjust))
                .andExpect(status().isOk());
    }

    @Test
    void pharmacistAtAnotherFacilityCannotDispense() throws Exception {
        Prescription prescription = pendingPrescriptionAtHomeFacility();
        String other = login("PHARM900", "Pharmacist@900");

        mvc.perform(post("/api/pharmacy/dispense/" + prescription.getId()).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/dispensing").header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "prescriptionId", prescription.getId(), "quantityDispensed", "10 tablets"))))
                .andExpect(status().isForbidden());

        assertThat(prescriptionRepository.findById(prescription.getId()).orElseThrow().getDispensed()).isFalse();
    }

    @Test
    void pharmacyLookupOnlyListsThisFacilitysPrescriptions() throws Exception {
        pendingPrescriptionAtHomeFacility();
        String other = login("PHARM900", "Pharmacist@900");
        MvcResult result = mvc.perform(get("/api/pharmacy/patient/" + SEEDED_PATIENT_ID).header("Authorization", bearer(other)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pending = json.readTree(result.getResponse().getContentAsString()).get("pendingPrescriptions");
        assertThat(pending).isEmpty();
    }

    // ---- P0-8: front desk never sees referral clinical detail --------------------

    @Test
    void frontDeskGetsReferralReportWithoutClinicalSummary() throws Exception {
        Patient patient = patientRepository.findByIdNumber(SEEDED_PATIENT_ID).orElseThrow();
        Long otherFacilityId = staffRepository.findByStaffNumber("PHARM900").orElseThrow().getFacility().getId();
        String doctor = login("DOC001", "Doctor@123");
        mvc.perform(post("/api/referrals").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "patientId", patient.getId(), "toFacilityId", otherFacilityId,
                                "urgency", "URGENT", "reason", "Cardiology review",
                                "clinicalSummary", "CONFIDENTIAL-SUMMARY"))))
                .andExpect(status().is2xxSuccessful());

        String admin = login("ADMIN001", "Admin@123");
        mvc.perform(get("/api/referrals/patient/" + patient.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/referrals/outgoing").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());

        String report = mvc.perform(get("/api/referrals/report").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(report).contains("Cardiology review").doesNotContain("CONFIDENTIAL-SUMMARY");
    }

    // ---- P1-8: only clinical intake opens a visit --------------------------------

    @Test
    void pharmacistCannotCreateAVisit() throws Exception {
        Patient patient = patientRepository.findByIdNumber(SEEDED_PATIENT_ID).orElseThrow();
        String pharmacist = login("PHARM001", "Pharmacist@123");
        mvc.perform(post("/api/visits").header("Authorization", bearer(pharmacist))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "patientId", patient.getId(), "facilityId", homeFacility.getId(), "reason", "test"))))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ------------------------------------------------------------------

    // ---- Each job in the right portal ----------------------------------------------

    @Test
    void onlyTheSystemAdminAddsFacilities() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        String facility = json.writeValueAsString(Map.of("name", "Barberton Clinic", "type", "CLINIC", "province", "Mpumalanga", "address", "Barberton"));
        mvc.perform(post("/api/facilities").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(facility))
                .andExpect(status().isForbidden());

        String sysAdmin = login("SYSADMIN001", "SysAdmin@123");
        mvc.perform(post("/api/facilities").header("Authorization", bearer(sysAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(facility))
                .andExpect(status().isCreated());
        // The same name twice is refused.
        mvc.perform(post("/api/facilities").header("Authorization", bearer(sysAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(facility))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheSystemAdminPostsNetworkWideAnnouncements() throws Exception {
        String notice = json.writeValueAsString(Map.of("title", "Clinic closed", "message", "Closed on Friday", "active", true));
        mvc.perform(post("/api/announcements").header("Authorization", bearer(login("ADMIN001", "Admin@123")))
                        .contentType(MediaType.APPLICATION_JSON).content(notice))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/announcements").header("Authorization", bearer(login("SYSADMIN001", "SysAdmin@123")))
                        .contentType(MediaType.APPLICATION_JSON).content(notice))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void managementReportsAreForTheSystemAdminNotTheFrontDesk() throws Exception {
        String admin = login("ADMIN001", "Admin@123");
        for (String path : new String[] {"/api/prescriptions/report", "/api/dispensing/report", "/api/lab-results/report", "/api/stock/report"}) {
            mvc.perform(get(path).header("Authorization", bearer(admin))).andExpect(status().isForbidden());
        }
        // The front desk keeps the referral report it uses for expected arrivals.
        mvc.perform(get("/api/referrals/report").header("Authorization", bearer(admin))).andExpect(status().isOk());
    }

    @Test
    void frontDeskAndPharmacistCannotMakeClinicalQueueDecisions() throws Exception {
        for (String token : new String[] {login("ADMIN001", "Admin@123"), login("PHARM001", "Pharmacist@123")}) {
            mvc.perform(post("/api/queue/1/urgency").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"urgency\":\"EMERGENCY\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/queue/1/complete").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }
    }

    private Prescription pendingPrescriptionAtHomeFacility() {
        return prescriptionRepository.findAll().stream()
                .filter(p -> "PHARMACY".equals(p.getDispenseMethod()))
                .filter(p -> !Boolean.TRUE.equals(p.getDispensed()))
                .filter(p -> p.getFacility().getId().equals(homeFacility.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data has no pending pharmacy prescription"));
    }

    private String newAccount(String staffNumber, String role) throws Exception {
        return json.writeValueAsString(Map.of(
                "staffNumber", staffNumber, "firstName", "Test", "lastName", "Account",
                "role", role, "facilityId", homeFacility.getId(),
                "email", staffNumber.toLowerCase() + "@test.local", "password", "TempPass1!"));
    }

    private String login(String staffNumber, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", staffNumber, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
