package com.udhr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicationDispenseTally {
    private String medicationName;
    private int dispenseCount;
    // Best-effort sum of the leading number in each dispense's free-text
    // quantity (e.g. "30 tablets" -> 30); entries that don't parse are
    // skipped, so this can undercount rather than be wrong.
    private int totalUnitsDispensed;
}
