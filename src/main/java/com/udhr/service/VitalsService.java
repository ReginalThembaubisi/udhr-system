package com.udhr.service;

import com.udhr.dto.VitalsRequest;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.Vitals;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.QueueEntryRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.VisitRepository;
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
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private VisitRepository visitRepository;

    public Vitals recordVitals(VitalsRequest request, String staffNumber) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        Vitals vitals = new Vitals();
        vitals.setPatient(patient);
        vitals.setFacility(staff.getFacility());
        vitals.setRecordedBy(staff);
        vitals.setSystolicBp(request.getSystolicBp());
        vitals.setDiastolicBp(request.getDiastolicBp());
        vitals.setTemperatureC(request.getTemperatureC());
        vitals.setPulseBpm(request.getPulseBpm());
        vitals.setRespiratoryRate(request.getRespiratoryRate());
        vitals.setOxygenSaturation(request.getOxygenSaturation());
        vitals.setWeightKg(request.getWeightKg());
        vitals.setHeightCm(request.getHeightCm());
        vitals.setGlucoseMmol(request.getGlucoseMmol());
        vitals.setNotes(request.getNotes());

        if (request.getQueueEntryId() != null) {
            queueEntryRepository.findById(request.getQueueEntryId()).ifPresent(vitals::setQueueEntry);
        }
        if (request.getVisitId() != null) {
            visitRepository.findById(request.getVisitId()).ifPresent(vitals::setVisit);
        }

        return vitalsRepository.save(vitals);
    }

    public List<Vitals> getVitalsByPatient(Long patientId) {
        return vitalsRepository.findByPatientIdOrderByRecordedAtDesc(patientId);
    }
}
