package com.udhr.dto;

import com.udhr.model.LabResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabResultReportResponse {
    private String facilityName;
    private int totalLabResults;
    private int resultsToday;
    private int uniquePatientsTested;
    private int uniqueTestTypes;
    private List<TestTypeTally> topTestTypes;
    private List<LabStaffTally> topOrderingStaff;
    private List<LabResult> recentResults;
}
