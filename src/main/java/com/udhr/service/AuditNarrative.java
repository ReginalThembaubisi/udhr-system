package com.udhr.service;

import com.udhr.model.AuditLog;
import java.util.Map;

/** Human-readable labels and event summaries. Action codes remain stable for filtering and integrations. */
public final class AuditNarrative {
    private AuditNarrative() {}

    private static final Map<String, String> TITLES = Map.ofEntries(
        Map.entry("ADMIN_VIEW_AUDIT_LOG", "Audit history reviewed"),
        Map.entry("LOGIN_SUCCESS", "Successful sign-in"),
        Map.entry("LOGIN_FAILED", "Unsuccessful sign-in"),
        Map.entry("LOGIN_LOCKED", "Staff account temporarily locked"),
        Map.entry("LOGOUT", "Staff signed out"),
        Map.entry("REGISTER_PATIENT", "Patient registered"),
        Map.entry("PATIENT_PORTAL_ACCOUNT_CREATED", "Patient portal account created"),
        Map.entry("VIEW_RECORD", "Patient medical record accessed"),
        Map.entry("VIEW_PATIENT_DATA", "Patient information accessed"),
        Map.entry("PATIENT_VIEW_RECORD", "Patient viewed their medical record"),
        Map.entry("PATIENT_LOGIN_SUCCESS", "Patient signed in"),
        Map.entry("PATIENT_LOGIN_FAILED", "Unsuccessful patient sign-in"),
        Map.entry("PATIENT_LOGIN_LOCKED", "Patient portal sign-in locked"),
        Map.entry("RECORD_VITALS", "Vital signs recorded"),
        Map.entry("ADD_LAB_RESULT", "Laboratory result recorded"),
        Map.entry("ADMINISTER_IMMUNIZATION", "Vaccine dose administered"),
        Map.entry("MARK_IMMUNIZATION_MISSED", "Immunization marked as missed"),
        Map.entry("ADD_IMMUNIZATION", "Immunization record added"),
        Map.entry("CREATE_REFERRAL", "Patient referred to another facility"),
        Map.entry("RESPOND_REFERRAL", "Referral response recorded"),
        Map.entry("REGISTER_VISIT", "Patient visit created"),
        Map.entry("CHECK_IN_PATIENT", "Patient checked in"),
        Map.entry("UPDATE_VISIT_STATUS", "Patient visit status updated"),
        Map.entry("DISCHARGE_PATIENT", "Patient discharged"),
        Map.entry("QUEUE_CHECK_IN", "Patient added to clinic queue"),
        Map.entry("QUEUE_URGENCY_UPDATED", "Queue priority changed"),
        Map.entry("QUEUE_CALLED", "Patient called for consultation"),
        Map.entry("QUEUE_PHARMACY", "Patient sent to pharmacy"),
        Map.entry("QUEUE_COMPLETED", "Queue entry completed"),
        Map.entry("QUEUE_CANCELLED", "Queue entry cancelled"),
        Map.entry("DISPENSE_MEDICATION", "Medication dispensed"),
        Map.entry("DISPENSE_PRESCRIPTION", "Prescription marked as dispensed"),
        Map.entry("ACCESS_OUTSIDE_QUEUE", "Record opened outside today's list (reason given)"),
        Map.entry("PASSWORD_RESET_CODE_SENT", "Password reset code emailed"),
        Map.entry("PASSWORD_RESET_FAILED", "Wrong password reset code entered"),
        Map.entry("PASSWORD_RESET_BY_EMAIL", "Password reset with an emailed code"),
        Map.entry("PASSWORD_RESET_LIMITED", "Forgot-password requests paused for an address"),
        Map.entry("PATIENT_PIN_RESET_CODE_SENT", "Patient asked for a PIN reset code by SMS"),
        Map.entry("PATIENT_PIN_RESET_FAILED", "Wrong PIN reset code entered"),
        Map.entry("PATIENT_PIN_RESET_BY_SMS", "Patient chose a new PIN using an SMS code"),
        Map.entry("PATIENT_PIN_RESET_LIMITED", "Forgot-PIN requests paused for an address"),
        Map.entry("SECURITY_ALERT_EMAILED", "Security alert emailed to the system admins"),
        Map.entry("SECURITY_ALERT_TEST_EMAIL", "Test security alert email sent"),
        Map.entry("ADMIN_CHANGED_EMAIL", "Staff email changed"),
        Map.entry("ADD_PRESCRIPTION", "Prescription issued"),
        Map.entry("ADD_DIAGNOSIS", "Diagnosis recorded"),
        Map.entry("ADD_ALLERGY", "Allergy recorded"),
        Map.entry("STOCK_RECEIVED", "Medication stock received"),
        Map.entry("STOCK_ADJUSTED", "Stock quantity adjusted"),
        Map.entry("STOCK_DISPENSED", "Medication stock dispensed"),
        Map.entry("STOCK_BULK_IMPORT", "Stock list imported"),
        Map.entry("ADD_STOCK_ITEM", "Medication added to stock"),
        Map.entry("CREATE_ANNOUNCEMENT", "Public announcement created"),
        Map.entry("UPDATE_ANNOUNCEMENT", "Public announcement updated"),
        Map.entry("DELETE_ANNOUNCEMENT", "Public announcement deleted"),
        Map.entry("CREATE_STAFF_ACCOUNT", "Staff account created"),
        Map.entry("ADMIN_VIEW_PATIENT_ACCOUNTS", "Patient account directory viewed"),
        Map.entry("ADMIN_ACTIVATED_ACCOUNT", "Staff account activated"),
        Map.entry("ADMIN_DEACTIVATED_ACCOUNT", "Staff account deactivated"),
        Map.entry("ADMIN_FORCE_LOGOUT", "Staff sessions ended"),
        Map.entry("ADMIN_RESET_PASSWORD", "Staff password reset"),
        Map.entry("REPORT_CSV_EXPORTED", "Report downloaded as CSV"),
        Map.entry("REPORT_VIEWED", "Management report reviewed"),
        Map.entry("DISPENSING_BLOCKED_EXPIRED", "Expired medication dispensing blocked"),
        Map.entry("STOCK_RECEIPT_REJECTED", "Expired medication delivery rejected"),
        Map.entry("PATIENT_VIEW_PROFILE", "Patient profile viewed"),
        Map.entry("ADD_CHRONIC_CONDITION", "Chronic condition recorded"),
        Map.entry("UPDATE_MEDICATION_ADHERENCE", "Medication adherence updated"),
        Map.entry("PATIENT_SYMPTOM_CHECK", "Patient symptom assessment completed"),
        Map.entry("SECURITY_ACCESS_DENIED", "Restricted access denied")
    );

    public static String title(String action) {
        if (action == null || action.isBlank()) return "System activity";
        String named = TITLES.get(action);
        if (named != null) return named;
        String readable = action.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }

    public static String summary(AuditLog log) {
        String description = log.getDescription();
        String actor = log.getActorLabel();
        if (actor == null || actor.isBlank()) {
            if (log.getStaff() != null) {
                var staff = log.getStaff();
                actor = readableRole(staff.getRole()) + " " + staff.getFirstName() + " " + staff.getLastName()
                        + " (" + staff.getStaffNumber() + ")";
            } else if (log.getPatient() != null) {
                actor = "Patient " + log.getPatient().getMrn();
            } else {
                actor = "Unidentified visitor";
            }
        }
        // General system oversight is not authority to read clinical details.
        // Free-text clinical descriptions and historical entries may contain notes.
        String action = log.getAction();
        if (action != null && (action.equals("ADD_ALLERGY") || action.equals("ALLERGY_OVERRIDE")
                || action.equals("ADD_PRESCRIPTION") || action.equals("ADD_DIAGNOSIS")
                || action.equals("DISPENSE_PRESCRIPTION") || action.equals("DISPENSE_MEDICATION")
                || action.equals("ADD_LAB_RESULT") || action.equals("RECORD_VITALS")
                || action.equals("PATIENT_SYMPTOM_CHECK") || action.equals("DISPENSING_BLOCKED_EXPIRED"))) {
            return actor + " " + title(action).toLowerCase(java.util.Locale.ROOT)
                    + (log.getPatient() != null ? " for patient " + log.getPatient().getMrn() : "")
                    + (log.getEntityId() != null ? " (record #" + log.getEntityId() + ")" : "") + ".";
        }
        if (description == null || description.isBlank()) return actor + " — " + title(log.getAction()).toLowerCase(java.util.Locale.ROOT) + ".";
        String detail = description.trim();
        // Some older events already begin with the actor. Do not duplicate their identity.
        if (detail.toLowerCase(java.util.Locale.ROOT).startsWith(actor.toLowerCase(java.util.Locale.ROOT))) return detail;
        return actor + " " + Character.toLowerCase(detail.charAt(0)) + detail.substring(1)
                + (detail.endsWith(".") ? "" : ".");
    }

    public static java.util.List<String> matchingCodes(String query) {
        if (query == null || query.isBlank()) return java.util.List.of();
        String lower = query.toLowerCase(java.util.Locale.ROOT);
        return TITLES.entrySet().stream()
                .filter(e -> e.getValue().toLowerCase(java.util.Locale.ROOT).contains(lower))
                .map(Map.Entry::getKey).toList();
    }

    public static String readableRole(String role) {
        if (role == null) return "Staff member";
        return switch (role) {
            case "SYSTEM_ADMIN" -> "System Administrator";
            case "ADMIN" -> "Receptionist";
            case "DOCTOR" -> "Doctor";
            case "NURSE" -> "Nurse";
            case "PHARMACIST" -> "Pharmacist";
            case "PATIENT" -> "Patient";
            default -> "Staff member";
        };
    }
}
