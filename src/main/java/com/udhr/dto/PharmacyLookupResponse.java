package com.udhr.dto;

import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Visit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PharmacyLookupResponse {
    private Patient patient;
    private Visit currentVisit; // shows which doctor/facility the patient is coming from
    private List<Prescription> pendingPrescriptions; // dispenseMethod=PHARMACY, not yet dispensed
}
