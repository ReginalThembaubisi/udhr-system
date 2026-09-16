package com.udhr.service;

import com.udhr.dto.ReferralRequest;
import com.udhr.dto.ReferralResponseRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReferralService {

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Transactional
    public Referral createReferral(ReferralRequest request, String staffNumber) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility toFacility = facilityRepository.findById(request.getToFacilityId())
                .orElseThrow(() -> new RuntimeException("Destination facility not found"));

        if (toFacility.getId().equals(staff.getFacility().getId())) {
            throw new RuntimeException("Cannot refer a patient to their current facility");
        }

        Visit visit;
        if (request.getVisitId() != null) {
            visit = visitRepository.findById(request.getVisitId())
                    .orElseThrow(() -> new RuntimeException("Visit not found"));
        } else {
            visit = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId()).stream()
                    .filter(v -> v.getStatus() == Visit.Status.ACTIVE)
                    .findFirst()
                    .orElse(null);
            if (visit == null) {
                visit = new Visit();
                visit.setPatient(patient);
                visit.setStaff(staff);
                visit.setFacility(staff.getFacility());
                visit.setReason("Clinical consultation");
                visit.setNotes("Automatically created for referral.");
                visit = visitRepository.save(visit);
            }
        }

        Referral referral = new Referral();
        referral.setPatient(patient);
        referral.setFromFacility(staff.getFacility());
        referral.setToFacility(toFacility);
        referral.setReferredBy(staff);
        referral.setVisit(visit);
        referral.setUrgency(request.getUrgency() != null && !request.getUrgency().isBlank()
                ? Referral.Urgency.valueOf(request.getUrgency())
                : Referral.Urgency.ROUTINE);
        referral.setReason(request.getReason());
        referral.setClinicalSummary(request.getClinicalSummary());
        Referral saved = referralRepository.save(referral);

        // The originating visit's disposition is "referred elsewhere" — the
        // Referral record itself carries the destination and clinical detail.
        visit.setStatus(Visit.Status.REFERRED);
        visit.setDischargedBy(staff);
        visit.setDischargedAt(LocalDateTime.now());
        visitRepository.save(visit);

        return saved;
    }

    public List<Referral> getIncoming(String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        return referralRepository.findByToFacilityIdOrderByReferredAtDesc(staff.getFacility().getId());
    }

    public List<Referral> getOutgoing(String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        return referralRepository.findByFromFacilityIdOrderByReferredAtDesc(staff.getFacility().getId());
    }

    @Transactional
    public Referral respond(Long id, ReferralResponseRequest request, String staffNumber) {
        Referral referral = referralRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Referral not found"));
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        referral.setStatus(Referral.Status.valueOf(request.getStatus()));
        referral.setResponseNotes(request.getResponseNotes());
        referral.setRespondedBy(staff);
        referral.setRespondedAt(LocalDateTime.now());

        return referralRepository.save(referral);
    }

    public List<Referral> getPatientHistory(Long patientId) {
        return referralRepository.findByPatientIdOrderByReferredAtDesc(patientId);
    }
}
