package com.udhr.service;

import com.udhr.dto.AllergyRequest;
import com.udhr.model.Allergy;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.repository.AllergyRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.security.CurrentUser;
import com.udhr.security.FacilityGuard;
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
    private StaffRepository staffRepository;

    private Staff currentStaff() {
        return staffRepository.findByStaffNumber(CurrentUser.principal())
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));
    }

    public Allergy addAllergy(AllergyRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        FacilityGuard.assertSameFacility(currentStaff(), patient);

        Allergy allergy = new Allergy();
        allergy.setPatient(patient);
        allergy.setAllergen(request.getAllergen());
        allergy.setSeverity(request.getSeverity());
        allergy.setNotes(request.getNotes());

        return allergyRepository.save(allergy);
    }

    public List<Allergy> getAllergiesByPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        FacilityGuard.assertSameFacility(currentStaff(), patient);
        return allergyRepository.findByPatientId(patientId);
    }
}
