package com.udhr.service;

import com.udhr.dto.LabResultReportResponse;
import com.udhr.dto.LabResultRequest;
import com.udhr.dto.LabStaffTally;
import com.udhr.dto.TestTypeTally;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.util.DateRangeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabResultService {

    @Autowired
    private LabResultRepository labResultRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private VisitRepository visitRepository;

    public LabResult addLabResult(LabResultRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Staff staff;
        if (request.getStaffId() != null) {
            staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new RuntimeException("Staff not found"));
        } else {
            String staffNum = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            staff = staffRepository.findByStaffNumber(staffNum)
                    .orElseThrow(() -> new RuntimeException("Logged in staff not found"));
        }

        Facility facility;
        if (request.getFacilityId() != null) {
            facility = facilityRepository.findById(request.getFacilityId())
                    .orElseThrow(() -> new RuntimeException("Facility not found"));
        } else {
            facility = staff.getFacility();
        }

        Visit visit;
        if (request.getVisitId() != null) {
            visit = visitRepository.findById(request.getVisitId())
                    .orElseThrow(() -> new RuntimeException("Visit not found"));
        } else {
            List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
            if (!visits.isEmpty()) {
                visit = visits.get(0);
            } else {
                visit = new Visit();
                visit.setPatient(patient);
                visit.setStaff(staff);
                visit.setFacility(facility);
                visit.setReason("Clinical consultation");
                visit.setNotes("Automatically created for lab result log.");
                visit = visitRepository.save(visit);
            }
        }

        LabResult labResult = new LabResult();
        labResult.setPatient(patient);
        labResult.setStaff(staff);
        labResult.setFacility(facility);
        labResult.setVisit(visit);
        labResult.setTestName(request.getTestName());
        labResult.setResult(request.getResult());
        labResult.setUnit(request.getUnit());
        labResult.setNormalRange(request.getNormalRange());
        labResult.setNotes(request.getNotes());

        return labResultRepository.save(labResult);
    }

    public List<LabResult> getLabResultsByPatient(Long patientId) {
        return labResultRepository.findByPatientIdOrderByTestDateDesc(patientId);
    }

    public LabResultReportResponse getReport(String staffNumber, String startDateStr, String endDateStr) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility facility = staff.getFacility();
        LocalDate startDate = DateRangeUtil.parseOrNull(startDateStr);
        LocalDate endDate = DateRangeUtil.parseOrNull(endDateStr);

        List<LabResult> allEver = labResultRepository.findByFacilityIdOrderByTestDateDesc(facility.getId());

        // "Results Today" is an always-live pulse metric, independent of
        // whatever historical range is being browsed.
        LocalDate today = LocalDate.now();
        int resultsToday = (int) allEver.stream()
                .filter(r -> r.getTestDate().toLocalDate().equals(today))
                .count();

        List<LabResult> all = allEver.stream()
                .filter(r -> DateRangeUtil.isWithinRange(r.getTestDate(), startDate, endDate))
                .collect(Collectors.toList());

        long uniquePatients = all.stream()
                .map(r -> r.getPatient().getId())
                .distinct()
                .count();

        Map<String, Integer> testTypeCounts = new LinkedHashMap<>();
        Map<String, Integer> staffCounts = new LinkedHashMap<>();
        for (LabResult r : all) {
            testTypeCounts.merge(r.getTestName(), 1, Integer::sum);
            String staffName = r.getStaff().getFirstName() + " " + r.getStaff().getLastName();
            staffCounts.merge(staffName, 1, Integer::sum);
        }

        List<TestTypeTally> topTestTypes = testTypeCounts.entrySet().stream()
                .map(e -> new TestTypeTally(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(TestTypeTally::getTestCount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<LabStaffTally> topOrderingStaff = staffCounts.entrySet().stream()
                .map(e -> new LabStaffTally(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(LabStaffTally::getTestCount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<LabResult> recent = all.stream().limit(10).collect(Collectors.toList());

        return new LabResultReportResponse(
                facility.getName(),
                all.size(),
                resultsToday,
                (int) uniquePatients,
                testTypeCounts.size(),
                topTestTypes,
                topOrderingStaff,
                recent
        );
    }
}
