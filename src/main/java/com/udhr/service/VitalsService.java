package com.udhr.service;

import com.udhr.dto.VitalsRequest;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.model.Vitals;
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
    private PatientService patientService;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private VisitService visitService;

    /**
     * Nurse looks the patient up by ID number or MRN and records vitals in one
     * step. No queue ticket is needed: this opens (or reuses) the patient's
     * current visit and immediately hands it off to the doctor by moving the
     * visit status to VITALS_DONE.
     */
    public Vitals recordVitals(VitalsRequest request, String staffNumber) {
        validate(request);
        Patient patient = patientService.findByIdentifier(request.getIdNumber());

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

    private static final java.util.regex.Pattern BLOOD_PRESSURE = java.util.regex.Pattern.compile("^\\s*(\\d{2,3})\\s*/\\s*(\\d{2,3})\\s*$");

    /**
     * Refuses readings that can't be real (BP "hello", 50 °C, SpO2 150 % were
     * all accepted before), so a typo never reaches the doctor as a vital sign.
     * The limits are wide on purpose: they catch impossible values, not
     * abnormal ones, which are flagged on screen instead.
     */
    private void validate(VitalsRequest r) {
        java.util.List<String> problems = new java.util.ArrayList<>();
        if (r.getBloodPressure() != null && !r.getBloodPressure().isBlank()) {
            java.util.regex.Matcher m = BLOOD_PRESSURE.matcher(r.getBloodPressure());
            if (!m.matches()) {
                problems.add("blood pressure must look like 120/80");
            } else {
                int systolic = Integer.parseInt(m.group(1));
                int diastolic = Integer.parseInt(m.group(2));
                if (systolic < 50 || systolic > 260 || diastolic < 20 || diastolic > 160 || diastolic >= systolic) {
                    problems.add("blood pressure " + r.getBloodPressure().trim() + " isn't a possible reading");
                }
            }
        }
        range(problems, "temperature", r.getTemperatureCelsius(), 30, 45, "°C");
        range(problems, "pulse", r.getPulseBpm(), 20, 250, "bpm");
        range(problems, "respiration rate", r.getRespirationRate(), 4, 70, "breaths/min");
        range(problems, "SpO2", r.getOxygenSaturation(), 50, 100, "%");
        range(problems, "weight", r.getWeightKg(), 0.3, 400, "kg");
        range(problems, "height", r.getHeightCm(), 20, 250, "cm");
        if (!problems.isEmpty()) {
            throw new RuntimeException("Please check the readings: " + String.join("; ", problems) + ".");
        }
    }

    private static void range(java.util.List<String> problems, String name, Number value, double min, double max, String unit) {
        if (value != null && (value.doubleValue() < min || value.doubleValue() > max)) {
            problems.add(name + " must be between " + fmt(min) + " and " + fmt(max) + " " + unit);
        }
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
