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

    public Allergy addAllergy(AllergyRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Allergy allergy = new Allergy();
        allergy.setPatient(patient);
        allergy.setAllergen(request.getAllergen());
        allergy.setSeverity(request.getSeverity());
        allergy.setNotes(request.getNotes());

        return allergyRepository.save(allergy);
    }

    public List<Allergy> getAllergiesByPatient(Long patientId) {
        return allergyRepository.findByPatientId(patientId);
    }
}
