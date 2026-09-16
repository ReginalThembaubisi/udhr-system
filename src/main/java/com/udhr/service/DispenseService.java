package com.udhr.service;

import com.udhr.dto.DispenseReportResponse;
import com.udhr.dto.DispenseRequest;
import com.udhr.dto.MedicationDispenseTally;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DispenseService {

    private static final Pattern LEADING_INT = Pattern.compile("^\\s*(\\d+)");

    @Autowired
    private DispenseRepository dispenseRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StockService stockService;

    @Transactional
    public Dispense dispense(DispenseRequest request, String staffNumber) {
        Prescription prescription = prescriptionRepository.findById(request.getPrescriptionId())
                .orElseThrow(() -> new RuntimeException("Prescription not found"));
        if (!prescription.getActive()) {
            throw new RuntimeException("Cannot dispense an inactive or expired prescription");
        }

        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        // Best-effort: draws down facility stock for this medication if it's
        // being tracked there. Throws (before any Dispense record is
        // created) if the facility tracks it but doesn't have enough on hand.
        stockService.decrementForDispense(staff.getFacility(), prescription.getMedication(),
                request.getQuantityDispensed(), staff, prescription.getId());

        Dispense dispense = new Dispense();
        dispense.setPrescription(prescription);
        dispense.setPatient(prescription.getPatient());
        dispense.setFacility(staff.getFacility());
        dispense.setDispensedBy(staff);
        dispense.setQuantityDispensed(request.getQuantityDispensed());
        dispense.setDaysSupply(request.getDaysSupply());
        dispense.setPharmacyNotes(request.getPharmacyNotes());

        return dispenseRepository.save(dispense);
    }

    public List<Dispense> getHistoryForPrescription(Long prescriptionId) {
        return dispenseRepository.findByPrescriptionIdOrderByDispensedAtDesc(prescriptionId);
    }

    public List<Dispense> getHistoryForPatient(Long patientId) {
        return dispenseRepository.findByPatientIdOrderByDispensedAtDesc(patientId);
    }

    public DispenseReportResponse getReport(String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility facility = staff.getFacility();

        List<Dispense> all = dispenseRepository.findByFacilityIdOrderByDispensedAtDesc(facility.getId());

        LocalDate today = LocalDate.now();
        int dispensedToday = (int) all.stream()
                .filter(d -> d.getDispensedAt().toLocalDate().equals(today))
                .count();

        long uniquePatients = all.stream()
                .map(d -> d.getPatient().getId())
                .distinct()
                .count();

        Map<String, MedicationDispenseTally> tallies = new LinkedHashMap<>();
        for (Dispense d : all) {
            String medication = d.getPrescription().getMedication();
            MedicationDispenseTally tally = tallies.computeIfAbsent(medication, m -> new MedicationDispenseTally(m, 0, 0));
            tally.setDispenseCount(tally.getDispenseCount() + 1);

            Matcher matcher = LEADING_INT.matcher(d.getQuantityDispensed() == null ? "" : d.getQuantityDispensed());
            if (matcher.find()) {
                tally.setTotalUnitsDispensed(tally.getTotalUnitsDispensed() + Integer.parseInt(matcher.group(1)));
            }
        }
        List<MedicationDispenseTally> topMedications = tallies.values().stream()
                .sorted(Comparator.comparingInt(MedicationDispenseTally::getDispenseCount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<Dispense> recent = all.stream().limit(10).collect(Collectors.toList());

        return new DispenseReportResponse(
                facility.getName(),
                all.size(),
                dispensedToday,
                (int) uniquePatients,
                topMedications,
                recent
        );
    }
}
