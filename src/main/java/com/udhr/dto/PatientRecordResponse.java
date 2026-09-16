package com.udhr.dto;

import com.udhr.model.Allergy;
import com.udhr.model.ChronicCondition;
import com.udhr.model.Diagnosis;
import com.udhr.model.Immunization;
import com.udhr.model.LabResult;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Referral;
import com.udhr.model.Vitals;
import com.udhr.model.Visit;
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
    private List<Immunization> immunizations;
    private List<Vitals> vitals;
    private List<Referral> referrals;
}
