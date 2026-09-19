package com.udhr.service;

import com.udhr.dto.LabResultReportResponse;
import com.udhr.dto.LabResultRequest;
import com.udhr.dto.LabStaffTally;
import com.udhr.dto.TestTypeTally;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.util.CsvUtil;
import com.udhr.util.DateRangeUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
    private PatientService patientService;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private VisitService visitService;

    /**
     * Staff (nurse or doctor) looks the patient up by ID number or MRN and
     * pastes the result straight in — same "find patient, act on their
     * current visit" pattern as vitals, so nobody needs to know a visit ID
     * or facility ID.
     */
    public LabResult addLabResult(LabResultRequest request, String staffNumber) {
        Patient patient = patientService.findByIdentifier(request.getIdNumber());

        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Logged in staff not found"));

        Visit visit = visitService.findOrCreateOpenVisit(patient, staff, "Lab test");

        LabResult labResult = new LabResult();
        labResult.setPatient(patient);
        labResult.setStaff(staff);
        labResult.setFacility(staff.getFacility());
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

    public String exportCsv(String staffNumber, String startDateStr, String endDateStr) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility facility = staff.getFacility();
        LocalDate startDate = DateRangeUtil.parseOrNull(startDateStr);
        LocalDate endDate = DateRangeUtil.parseOrNull(endDateStr);

        List<LabResult> results = labResultRepository.findByFacilityIdOrderByTestDateDesc(facility.getId())
                .stream()
                .filter(r -> DateRangeUtil.isWithinRange(r.getTestDate(), startDate, endDate))
                .collect(Collectors.toList());

        List<String> headers = List.of("Date", "Test Name", "Result", "Unit", "Normal Range", "Patient", "Staff", "Notes");
        List<List<String>> rows = results.stream()
                .map(r -> List.of(
                        r.getTestDate().toString(),
                        r.getTestName(),
                        r.getResult() != null ? r.getResult() : "",
                        r.getUnit() != null ? r.getUnit() : "",
                        r.getNormalRange() != null ? r.getNormalRange() : "",
                        r.getPatient().getFirstName() + " " + r.getPatient().getLastName(),
                        r.getStaff().getFirstName() + " " + r.getStaff().getLastName(),
                        r.getNotes() != null ? r.getNotes() : ""
                ))
                .collect(Collectors.toList());

        return CsvUtil.buildCsv(headers, rows);
    }
}
