package com.udhr.service;

import com.udhr.dto.AllergyRequest;
import com.udhr.model.Allergy;
import com.udhr.model.Patient;
import com.udhr.repository.AllergyRepository;
import com.udhr.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AllergyService {

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AuditService auditService;

    public Allergy addAllergy(AllergyRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        if (request.getAllergen() == null || request.getAllergen().isBlank()) {
            throw new RuntimeException("Allergen is required");
        }
        String severity = request.getSeverity() != null ? request.getSeverity().trim().toUpperCase() : "";
        if (!List.of("MILD", "MODERATE", "SEVERE").contains(severity)) {
            throw new RuntimeException("Severity must be MILD, MODERATE or SEVERE");
        }

        Allergy allergy = new Allergy();
        allergy.setPatient(patient);
        allergy.setAllergen(request.getAllergen().trim());
        allergy.setSeverity(severity);
        allergy.setNotes(request.getNotes());

        Allergy saved = allergyRepository.save(allergy);
        auditService.recordCreate(auditService.currentStaff(), patient, "ADD_ALLERGY",
                "Recorded an allergy to " + saved.getAllergen(),
                saved.getSeverity() + (saved.getNotes() != null && !saved.getNotes().isBlank() ? " — " + saved.getNotes() : ""));
        return saved;
    }

    public List<Allergy> getAllergiesByPatient(Long patientId) {
        return allergyRepository.findByPatientId(patientId);
    }
}
