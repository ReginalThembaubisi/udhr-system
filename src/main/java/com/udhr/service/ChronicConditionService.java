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

        return chronicConditionRepository.save(chronicCondition);
    }

    public List<ChronicCondition> getChronicConditionsByPatient(Long patientId) {
        return chronicConditionRepository.findByPatientId(patientId);
    }
}
