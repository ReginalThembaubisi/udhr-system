package com.udhr.service;

import com.udhr.dto.DispenseRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DispenseService {

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
}
