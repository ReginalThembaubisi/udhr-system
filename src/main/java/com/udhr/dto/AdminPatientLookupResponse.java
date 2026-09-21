package com.udhr.dto;

import com.udhr.model.Patient;
import com.udhr.model.Visit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// What Front Desk (Admin) is allowed to see about a patient who isn't on
// today's list: who they are and their visit/check-in history. Deliberately
// leaves out diagnoses, prescriptions, vitals, allergies and labs — that
// stays clinical-staff-only (see /api/patients/*/record).
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminPatientLookupResponse {
    private Patient patient;
    private List<Visit> visits;
    private boolean firstVisit;
}
