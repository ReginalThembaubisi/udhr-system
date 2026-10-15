package com.udhr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.udhr.model.Allergy;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Visit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PharmacyLookupResponse {
    private Patient patient;
    private Visit currentVisit; // shows which doctor/facility the patient is coming from
    private List<Prescription> pendingPrescriptions; // dispenseMethod=PHARMACY, not yet dispensed

    // Shown at the top of the dispense screen: the pharmacist is the last check
    // before a medicine reaches the patient, so they need the allergies too.
    @JsonIgnoreProperties("patient")
    private List<Allergy> allergies;

    // Prescription id -> allergy warnings for that medication (absent when none).
    private Map<Long, List<String>> allergyWarnings;
}
