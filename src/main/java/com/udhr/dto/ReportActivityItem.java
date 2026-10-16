package com.udhr.dto;

import com.udhr.model.Dispense;
import com.udhr.model.LabResult;
import com.udhr.model.Prescription;

import java.time.LocalDateTime;

/**
 * One row of "recent activity" in the prescription, dispensing and lab reports.
 *
 * These reports are for running the facility (front desk and system
 * administrator), not for treating anyone, so a row says what happened and who
 * did it, never which patient it was. They used to return the whole entity,
 * which carried the patient's full record (ID number, address, phone), clinical
 * notes and lab values. POPIA section 10: only the personal information the
 * purpose needs.
 */
public record ReportActivityItem(Long id, LocalDateTime at, String item, String detail, String by) {

    public static ReportActivityItem from(Prescription p) {
        return new ReportActivityItem(p.getId(), p.getCreatedAt(), p.getMedication(),
                Boolean.TRUE.equals(p.getActive()) ? "Active" : "Inactive", fullName(p.getDoctor()));
    }

    public static ReportActivityItem from(Dispense d) {
        return new ReportActivityItem(d.getId(), d.getDispensedAt(), d.getPrescription().getMedication(),
                d.getQuantityDispensed(), fullName(d.getDispensedBy()));
    }

    public static ReportActivityItem from(LabResult r) {
        return new ReportActivityItem(r.getId(), r.getTestDate(), r.getTestName(), null, fullName(r.getStaff()));
    }

    private static String fullName(com.udhr.model.Staff s) {
        return s == null ? "" : s.getFirstName() + " " + s.getLastName();
    }
}
