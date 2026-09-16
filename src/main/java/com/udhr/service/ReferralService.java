package com.udhr.service;

import com.udhr.dto.FacilityReferralTally;
import com.udhr.dto.ReferralReportResponse;
import com.udhr.dto.ReferralRequest;
import com.udhr.dto.ReferralResponseRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.util.DateRangeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public ReferralReportResponse getReport(String staffNumber, String startDateStr, String endDateStr) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility facility = staff.getFacility();
        LocalDate startDate = DateRangeUtil.parseOrNull(startDateStr);
        LocalDate endDate = DateRangeUtil.parseOrNull(endDateStr);

        List<Referral> outgoing = referralRepository.findByFromFacilityIdOrderByReferredAtDesc(facility.getId())
                .stream()
                .filter(r -> DateRangeUtil.isWithinRange(r.getReferredAt(), startDate, endDate))
                .collect(Collectors.toList());
        List<Referral> incoming = referralRepository.findByToFacilityIdOrderByReferredAtDesc(facility.getId())
                .stream()
                .filter(r -> DateRangeUtil.isWithinRange(r.getReferredAt(), startDate, endDate))
                .collect(Collectors.toList());

        int pendingIncoming = (int) incoming.stream()
                .filter(r -> r.getStatus() == Referral.Status.PENDING)
                .count();
        int emergencyReferrals = (int) Stream.concat(outgoing.stream(), incoming.stream())
                .filter(r -> r.getUrgency() == Referral.Urgency.EMERGENCY)
                .count();

        List<FacilityReferralTally> topDestinations = tallyByFacility(outgoing, Referral::getToFacility);
        List<FacilityReferralTally> topSources = tallyByFacility(incoming, Referral::getFromFacility);

        List<Referral> recentActivity = Stream.concat(outgoing.stream(), incoming.stream())
                .sorted(Comparator.comparing(Referral::getReferredAt).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return new ReferralReportResponse(
                facility.getName(),
                outgoing.size(),
                incoming.size(),
                pendingIncoming,
                emergencyReferrals,
                topDestinations,
                topSources,
                recentActivity
        );
    }

    private List<FacilityReferralTally> tallyByFacility(List<Referral> referrals, java.util.function.Function<Referral, Facility> facilityOf) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Referral r : referrals) {
            String name = facilityOf.apply(r).getName();
            counts.merge(name, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .map(e -> new FacilityReferralTally(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(FacilityReferralTally::getReferralCount).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}
