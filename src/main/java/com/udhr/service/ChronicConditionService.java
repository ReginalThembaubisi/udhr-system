package com.udhr.service;

import com.udhr.dto.ChronicConditionRequest;
import com.udhr.model.ChronicCondition;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.repository.ChronicConditionRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.security.CurrentUser;
import com.udhr.security.FacilityGuard;
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

    @Autowired
    private StaffRepository staffRepository;

    private Staff currentStaff() {
        return staffRepository.findByStaffNumber(CurrentUser.principal())
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));
    }

    public ChronicCondition addChronicCondition(ChronicConditionRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        FacilityGuard.assertSameFacility(currentStaff(), patient);

        ChronicCondition chronicCondition = new ChronicCondition();
        chronicCondition.setPatient(patient);
        chronicCondition.setConditionName(request.getConditionName());
        chronicCondition.setDiagnosedDate(LocalDate.parse(request.getDiagnosedDate()));
        chronicCondition.setNotes(request.getNotes());

        return chronicConditionRepository.save(chronicCondition);
    }

    public List<ChronicCondition> getChronicConditionsByPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        FacilityGuard.assertSameFacility(currentStaff(), patient);
        return chronicConditionRepository.findByPatientId(patientId);
    }
}
