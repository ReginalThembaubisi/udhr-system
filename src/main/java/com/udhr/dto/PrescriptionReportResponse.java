package com.udhr.dto;

import com.udhr.model.Prescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrescriptionReportResponse {
    private String facilityName;
    private int totalPrescriptions;
    private int activePrescriptions;
    private int issuedToday;
    private int uniquePatientsPrescribed;
    private List<MedicationPrescriptionTally> topMedications;
    private List<PrescriberTally> topPrescribers;
    private List<Prescription> recentPrescriptions;
}
