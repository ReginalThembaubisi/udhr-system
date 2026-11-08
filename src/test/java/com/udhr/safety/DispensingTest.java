package com.udhr.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Staff;
import com.udhr.model.StockItem;
import com.udhr.model.Visit;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.PrescriptionRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.StockItemRepository;
import com.udhr.repository.VisitRepository;
import com.udhr.util.DispenseQuantity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dispensing has one path (fix list P1-9 to P1-11): it works out the quantity,
 * takes it from the matching stock item, closes the prescription, and refuses
 * one that is stopped, finished or already handed over. Medicine names are made
 * up or unique so these tests don't change what StockAvailabilityTest sees.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DispensingTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PatientRepository patientRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private VisitRepository visitRepository;

    @Test
    void theQuantityIsWorkedOutFromTheDoseFrequencyAndDays() {
        assertThat(DispenseQuantity.total(rx("2 tablets", "Twice daily", 5))).contains("20 tablets");
        assertThat(DispenseQuantity.total(rx("1 capsule", "Three times daily", 7))).contains("21 capsules");
        assertThat(DispenseQuantity.total(rx("1 tab", "Once daily", 1))).contains("1 tablet");
        // Not countable or not regular: the pharmacist types the quantity.
        assertThat(DispenseQuantity.total(rx("10 ml", "Three times daily", 5))).isEmpty();
        assertThat(DispenseQuantity.total(rx("2 tablets", "As needed", 5))).isEmpty();
    }

    @Test
    void dispensingTakesStockClosesThePrescriptionAndOnlyOnce() throws Exception {
        Staff pharmacist = staffRepository.findByStaffNumber("PHARM001").orElseThrow();
        StockItem stock = stock(pharmacist, "Zz Dispense Alpha 100mg", 500, LocalDate.now().plusYears(1));
        Prescription p = prescription(pharmacist, "Zz Dispense Alpha 100mg", "2 tablets", "Twice daily", 5, true);
        String token = login();

        JsonNode done = json.readTree(mvc.perform(post("/api/pharmacy/dispense/" + p.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(done.get("quantityDispensed").asText()).isEqualTo("20 tablets");
        assertThat(stockItemRepository.findById(stock.getId()).orElseThrow().getQuantityOnHand()).isEqualTo(480);
        assertThat(prescriptionRepository.findById(p.getId()).orElseThrow().getDispensed()).isTrue();

        // A second press (or the old "Log dispense" form) can't hand it over again.
        mvc.perform(post("/api/pharmacy/dispense/" + p.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/dispensing").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("prescriptionId", p.getId(), "quantityDispensed", "20 tablets"))))
                .andExpect(status().isBadRequest());
        assertThat(stockItemRepository.findById(stock.getId()).orElseThrow().getQuantityOnHand()).isEqualTo(480);

        // The dispensing record is kept.
        JsonNode history = json.readTree(mvc.perform(get("/api/dispensing/prescription/" + p.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(history).hasSize(1);
    }

    @Test
    void aTypedQuantityWinsAndTheSameMedicineUnderAnotherNameIsUsed() throws Exception {
        Staff pharmacist = staffRepository.findByStaffNumber("PHARM001").orElseThrow();
        // Stocked as the generic; prescribed by brand (MedicationCatalog: Diflucan is fluconazole).
        StockItem stock = stock(pharmacist, "Fluconazole 150mg dispense-test", 100, LocalDate.now().plusYears(1));
        Prescription p = prescription(pharmacist, "Diflucan", "1 tablet", "Once daily", 3, true);
        String token = login();
        mvc.perform(post("/api/pharmacy/dispense/" + p.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantityDispensed\":\"6 tablets\"}"))
                .andExpect(status().isOk());
        assertThat(stockItemRepository.findById(stock.getId()).orElseThrow().getQuantityOnHand()).isEqualTo(94);
    }

    @Test
    void aStoppedOrFinishedPrescriptionIsRefused() throws Exception {
        Staff pharmacist = staffRepository.findByStaffNumber("PHARM001").orElseThrow();
        String token = login();
        Prescription stopped = prescription(pharmacist, "Zz Dispense Beta", "1 tablet", "Once daily", 5, false);
        mvc.perform(post("/api/pharmacy/dispense/" + stopped.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        Prescription finished = prescription(pharmacist, "Zz Dispense Gamma", "1 tablet", "Once daily", 5, true);
        finished.setStartDate(LocalDate.now().minusDays(20));
        finished.setEndDate(LocalDate.now().minusDays(15));
        prescriptionRepository.save(finished);
        String message = mvc.perform(post("/api/pharmacy/dispense/" + finished.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(message).contains("ended on");
        assertThat(prescriptionRepository.findById(finished.getId()).orElseThrow().getDispensed()).isFalse();
    }

    private static Prescription rx(String dosage, String frequency, int days) {
        Prescription p = new Prescription();
        p.setDosage(dosage);
        p.setFrequency(frequency);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(days));
        return p;
    }

    private StockItem stock(Staff pharmacist, String name, int quantity, LocalDate expiry) {
        StockItem item = new StockItem();
        item.setFacility(pharmacist.getFacility());
        item.setMedicationName(name);
        item.setUnit("tablets");
        item.setQuantityOnHand(quantity);
        item.setExpiryDate(expiry);
        return stockItemRepository.save(item);
    }

    private Prescription prescription(Staff pharmacist, String medication, String dosage, String frequency, int days, boolean active) {
        Patient patient = patientRepository.findByIdNumber("9001015000083").orElseThrow();
        Staff doctor = staffRepository.findByStaffNumber("DOC001").orElseThrow();
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStaff(doctor);
        visit.setFacility(pharmacist.getFacility());
        visit.setVisitDate(java.time.LocalDateTime.now());
        visit.setReason("Dispensing test");
        visit.setStatus("SENT_TO_PHARMACY");
        visit = visitRepository.save(visit);
        Prescription p = rx(dosage, frequency, days);
        p.setVisit(visit);
        p.setPatient(patient);
        p.setDoctor(doctor);
        p.setFacility(pharmacist.getFacility());
        p.setMedication(medication);
        p.setDispenseMethod("PHARMACY");
        p.setActive(active);
        return prescriptionRepository.save(p);
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "10.3.0.1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("staffNumber", "PHARM001", "password", "Pharmacist@123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
