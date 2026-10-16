package com.udhr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DispenseReportResponse {
    private String facilityName;
    private int totalDispenseEvents;
    private int dispensedToday;
    private int uniquePatientsServed;
    private List<MedicationDispenseTally> topMedications;
    private List<ReportActivityItem> recentDispenses;
}
