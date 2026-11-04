package com.udhr.service;

import com.udhr.dto.ChronicConditionRequest;
import com.udhr.model.ChronicCondition;
import com.udhr.model.Patient;
import com.udhr.repository.ChronicConditionRepository;
import com.udhr.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class ChronicConditionService {

    @Autowired
    private AuditService auditService;

    @Autowired
    private ChronicConditionRepository chronicConditionRepository;

    @Autowired
    private PatientRepository patientRepository;

    public ChronicCondition addChronicCondition(ChronicConditionRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        ChronicCondition chronicCondition = new ChronicCondition();
        chronicCondition.setPatient(patient);
        chronicCondition.setConditionName(request.getConditionName());
        chronicCondition.setDiagnosedDate(LocalDate.parse(request.getDiagnosedDate()));
        chronicCondition.setNotes(request.getNotes());

        ChronicCondition saved = chronicConditionRepository.save(chronicCondition);
        auditService.recordEvent(auditService.currentStaff(), patient, "ADD_CHRONIC_CONDITION",
                "added a chronic-condition record for patient " + patient.getMrn(),
                "CHRONIC_CONDITION", saved.getId());
        return saved;
    }

    public List<ChronicCondition> getChronicConditionsByPatient(Long patientId) {
        return chronicConditionRepository.findByPatientId(patientId);
    }
}
