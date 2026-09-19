package com.udhr.dto;

import com.udhr.model.Allergy;
import com.udhr.model.ChronicCondition;
import com.udhr.model.Diagnosis;
import com.udhr.model.LabResult;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Visit;
import com.udhr.model.Vitals;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientRecordResponse {
    private Patient patient;
    private List<Allergy> allergies;
    private List<ChronicCondition> chronicConditions;
    private List<Visit> visits;
    private List<Diagnosis> diagnoses;
    private List<Prescription> prescriptions;
    private List<LabResult> labResults;
    private List<Vitals> vitals;
    private Visit currentVisit; // most recent visit, so the doctor sees status (e.g. VITALS_DONE) at a glance
}
