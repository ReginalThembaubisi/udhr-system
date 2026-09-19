package com.udhr.service;

import com.udhr.dto.VitalsRequest;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.model.Vitals;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.VitalsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VitalsService {

    @Autowired
    private VitalsRepository vitalsRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private VisitService visitService;

    /**
     * Nurse looks the patient up by ID number and records vitals in one step.
     * No queue ticket is needed: this opens (or reuses) the patient's current
     * visit and immediately hands it off to the doctor by moving the visit
     * status to VITALS_DONE.
     */
    public Vitals recordVitals(VitalsRequest request, String staffNumber) {
        Patient patient = patientRepository.findByIdNumber(request.getIdNumber())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Staff nurse = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Logged in staff not found"));

        Visit visit = visitService.findOrCreateOpenVisit(patient, nurse,
                request.getReason() != null ? request.getReason() : "Vitals check");

        Vitals vitals = new Vitals();
        vitals.setPatient(patient);
        vitals.setVisit(visit);
        vitals.setRecordedBy(nurse);
        vitals.setBloodPressure(request.getBloodPressure());
        vitals.setTemperatureCelsius(request.getTemperatureCelsius());
        vitals.setPulseBpm(request.getPulseBpm());
        vitals.setRespirationRate(request.getRespirationRate());
        vitals.setOxygenSaturation(request.getOxygenSaturation());
        vitals.setWeightKg(request.getWeightKg());
        vitals.setHeightCm(request.getHeightCm());
        vitals.setNotes(request.getNotes());
        Vitals saved = vitalsRepository.save(vitals);

        // Hand the patient off — the doctor will see them show up under
        // "ready for consultation" without any queue ticket changing hands.
        visitService.updateStatus(visit, "VITALS_DONE");

        return saved;
    }

    public List<Vitals> getVitalsByPatient(Long patientId) {
        return vitalsRepository.findByPatientIdOrderByRecordedAtDesc(patientId);
    }

    public Vitals getLatestVitalsForVisit(Long visitId) {
        return vitalsRepository.findFirstByVisitIdOrderByRecordedAtDesc(visitId).orElse(null);
    }
}
