package com.udhr.service;

import com.udhr.audit.AccessReasonInterceptor;
import com.udhr.model.AuditLog;
import com.udhr.model.Staff;
import com.udhr.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns the audit trail from evidence afterwards into a warning now. Worked
 * out on request from the last days of the trail (nothing new is stored), so
 * a system administrator sees, without searching:
 *
 *  - an account opening an unusual number of patient records;
 *  - records opened outside today's list, with the reason given;
 *  - a sign-in from an address that account hasn't used before;
 *  - repeated failed sign-ins and lockouts;
 *  - a signed-in account repeatedly trying screens it isn't allowed;
 *  - sign-in attempts on deactivated accounts.
 */
@Service
public class SecurityAlertService {

    public record Alert(String severity, String type, Long staffId, String staffNumber, String staffName, String role,
                        String title, String detail, LocalDateTime firstSeen, LocalDateTime lastSeen, int count) {}

    private static final Set<String> RECORD_READS = Set.of("VIEW_PATIENT_DATA", "VIEW_RECORD", "PHARMACY_LOOKUP",
            AccessReasonInterceptor.ACTION);
    private static final Set<String> FAILED_SIGN_INS = Set.of("LOGIN_FAILED", "LOGIN_2FA_FAILED");
    private static final List<String> ACTIONS = List.of("VIEW_PATIENT_DATA", "VIEW_RECORD", "PHARMACY_LOOKUP",
            AccessReasonInterceptor.ACTION, "LOGIN_SUCCESS", "LOGIN_FAILED", "LOGIN_2FA_FAILED", "LOGIN_LOCKED",
            "LOGIN_BLOCKED", "SECURITY_ACCESS_DENIED");
    private static final int NEW_PLACE_HISTORY_DAYS = 30;

    private final AuditLogRepository auditLogRepository;

    @Value("${udhr.alerts.records-per-hour:15}")
    private int recordsPerHour;

    @Value("${udhr.alerts.records-per-day:40}")
    private int recordsPerDay;

    public SecurityAlertService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<Alert> alerts(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AuditLog> logs = auditLogRepository.findByActionInAndCreatedAtAfterOrderByCreatedAtAsc(
                ACTIONS, since.minusDays(NEW_PLACE_HISTORY_DAYS));
        List<AuditLog> recent = logs.stream().filter(l -> !l.getCreatedAt().isBefore(since)).toList();

        List<Alert> alerts = new ArrayList<>();
        manyRecords(recent, alerts);
        outsideTodaysList(recent, alerts);
        newPlace(logs, since, alerts);
        failedSignIns(recent, alerts);
        repeatedDenials(recent, alerts);
        deactivatedSignIns(recent, alerts);

        alerts.sort(Comparator.comparing((Alert a) -> "HIGH".equals(a.severity()) ? 0 : 1)
                .thenComparing(Alert::lastSeen, Comparator.reverseOrder()));
        return alerts;
    }

    /** One account opening many different patients within an hour, or in one day. */
    private void manyRecords(List<AuditLog> logs, List<Alert> alerts) {
        Map<Long, List<AuditLog>> reads = byStaff(logs.stream()
                .filter(l -> RECORD_READS.contains(l.getAction()) && l.getPatient() != null).toList());
        for (List<AuditLog> staffReads : reads.values()) {
            Staff staff = staffReads.get(0).getStaff();
            // Busiest hour: the most different patients within 60 minutes of each other.
            int bestHour = 0;
            LocalDateTime hourStart = null;
            LocalDateTime hourEnd = null;
            for (int i = 0; i < staffReads.size(); i++) {
                LocalDateTime start = staffReads.get(i).getCreatedAt();
                Set<Long> patients = new HashSet<>();
                LocalDateTime last = start;
                for (int j = i; j < staffReads.size() && !staffReads.get(j).getCreatedAt().isAfter(start.plusHours(1)); j++) {
                    patients.add(staffReads.get(j).getPatient().getId());
                    last = staffReads.get(j).getCreatedAt();
                }
                if (patients.size() > bestHour) {
                    bestHour = patients.size();
                    hourStart = start;
                    hourEnd = last;
                }
            }
            if (bestHour >= recordsPerHour) {
                alerts.add(alert("HIGH", "MANY_RECORDS", staff,
                        "Opened " + bestHour + " patients' records within an hour",
                        "More than usual for one person (alert at " + recordsPerHour + "). Check they were all their patients; "
                                + "if not, end their sessions and reset the password.",
                        hourStart, hourEnd, bestHour));
                continue;
            }
            Map<LocalDate, List<AuditLog>> perDay = staffReads.stream()
                    .collect(Collectors.groupingBy(l -> l.getCreatedAt().toLocalDate(), LinkedHashMap::new, Collectors.toList()));
            for (List<AuditLog> day : perDay.values()) {
                long patients = day.stream().map(l -> l.getPatient().getId()).distinct().count();
                if (patients >= recordsPerDay) {
                    alerts.add(alert("HIGH", "MANY_RECORDS", staff,
                            "Opened " + patients + " patients' records in one day",
                            "More than usual for one person (alert at " + recordsPerDay + " a day).",
                            day.get(0).getCreatedAt(), day.get(day.size() - 1).getCreatedAt(), (int) patients));
                }
            }
        }
    }

    /** Records opened with a reason because the patient wasn't on today's list. */
    private void outsideTodaysList(List<AuditLog> logs, List<Alert> alerts) {
        Map<Long, List<AuditLog>> opened = byStaff(logs.stream()
                .filter(l -> AccessReasonInterceptor.ACTION.equals(l.getAction())).toList());
        for (List<AuditLog> entries : opened.values()) {
            Map<LocalDate, List<AuditLog>> perDay = entries.stream()
                    .collect(Collectors.groupingBy(l -> l.getCreatedAt().toLocalDate(), LinkedHashMap::new, Collectors.toList()));
            for (List<AuditLog> day : perDay.values()) {
                // Patients, not entries: opening one record sends several reads at once.
                int patients = (int) day.stream().filter(l -> l.getPatient() != null)
                        .map(l -> l.getPatient().getId()).distinct().count();
                String reasons = day.stream()
                        .map(l -> (l.getPatient() != null ? l.getPatient().getMrn() + ": " : "") + reasonOf(l.getDescription()))
                        .distinct().limit(5).collect(Collectors.joining("; "));
                alerts.add(alert(patients >= 5 ? "HIGH" : "MEDIUM", "OUTSIDE_QUEUE", day.get(0).getStaff(),
                        "Opened " + patients + " record" + (patients == 1 ? "" : "s") + " of patients not on today's list",
                        "Reasons given: " + reasons, day.get(0).getCreatedAt(), day.get(day.size() - 1).getCreatedAt(), patients));
            }
        }
    }

    /** A sign-in from an address this account hasn't signed in from in the last 30 days. */
    private void newPlace(List<AuditLog> logs, LocalDateTime since, List<Alert> alerts) {
        Map<Long, List<AuditLog>> signIns = byStaff(logs.stream()
                .filter(l -> "LOGIN_SUCCESS".equals(l.getAction()) && l.getIpAddress() != null).toList());
        for (List<AuditLog> entries : signIns.values()) {
            Set<String> seen = new HashSet<>();
            for (AuditLog entry : entries) {
                boolean firstEver = seen.isEmpty();
                boolean newAddress = seen.add(entry.getIpAddress());
                if (newAddress && !firstEver && !entry.getCreatedAt().isBefore(since)) {
                    alerts.add(alert("MEDIUM", "NEW_PLACE", entry.getStaff(),
                            "Signed in from a new address: " + entry.getIpAddress(),
                            "This account hadn't signed in from there in the last " + NEW_PLACE_HISTORY_DAYS
                                    + " days. Fine if they changed computer or network; check with them if not.",
                            entry.getCreatedAt(), entry.getCreatedAt(), 1));
                }
            }
        }
    }

    /** Repeated wrong passwords or codes on one account, and every lockout. */
    private void failedSignIns(List<AuditLog> logs, List<Alert> alerts) {
        for (AuditLog lock : logs.stream().filter(l -> "LOGIN_LOCKED".equals(l.getAction())).toList()) {
            alerts.add(alert("HIGH", "LOCKED", lock.getStaff(),
                    lock.getStaff() != null ? "Account locked after repeated failed sign-ins" : "An address was locked after repeated failed sign-ins",
                    lock.getDescription() + (lock.getIpAddress() != null ? " (from " + lock.getIpAddress() + ")" : "")
                            + ". Someone may be guessing the password.",
                    lock.getCreatedAt(), lock.getCreatedAt(), 1));
        }
        Map<Long, List<AuditLog>> failures = byStaff(logs.stream()
                .filter(l -> FAILED_SIGN_INS.contains(l.getAction())).toList());
        for (List<AuditLog> entries : failures.values()) {
            Map<LocalDate, List<AuditLog>> perDay = entries.stream()
                    .collect(Collectors.groupingBy(l -> l.getCreatedAt().toLocalDate(), LinkedHashMap::new, Collectors.toList()));
            for (List<AuditLog> day : perDay.values()) {
                if (day.size() >= 3) {
                    Set<String> addresses = day.stream().map(AuditLog::getIpAddress).filter(a -> a != null).collect(Collectors.toSet());
                    alerts.add(alert("MEDIUM", "FAILED_SIGN_INS", day.get(0).getStaff(),
                            day.size() + " failed sign-ins in one day",
                            "Wrong password or authenticator code" + (addresses.isEmpty() ? "" : ", from " + String.join(", ", addresses)) + ".",
                            day.get(0).getCreatedAt(), day.get(day.size() - 1).getCreatedAt(), day.size()));
                }
            }
        }
    }

    /** A signed-in account repeatedly trying screens its role isn't allowed. */
    private void repeatedDenials(List<AuditLog> logs, List<Alert> alerts) {
        Map<Long, List<AuditLog>> denied = byStaff(logs.stream()
                .filter(l -> "SECURITY_ACCESS_DENIED".equals(l.getAction())).toList());
        for (List<AuditLog> entries : denied.values()) {
            Map<LocalDate, List<AuditLog>> perDay = entries.stream()
                    .collect(Collectors.groupingBy(l -> l.getCreatedAt().toLocalDate(), LinkedHashMap::new, Collectors.toList()));
            for (List<AuditLog> day : perDay.values()) {
                if (day.size() >= 5) {
                    alerts.add(alert("MEDIUM", "DENIED", day.get(0).getStaff(),
                            "Refused " + day.size() + " times for screens this role can't use",
                            "Someone using this account tried things outside its job, e.g. " + day.get(day.size() - 1).getDescription(),
                            day.get(0).getCreatedAt(), day.get(day.size() - 1).getCreatedAt(), day.size()));
                }
            }
        }
    }

    /** The right password for a deactivated account: someone still has it. */
    private void deactivatedSignIns(List<AuditLog> logs, List<Alert> alerts) {
        for (AuditLog entry : logs.stream().filter(l -> "LOGIN_BLOCKED".equals(l.getAction()) && l.getStaff() != null).toList()) {
            alerts.add(alert("MEDIUM", "DEACTIVATED_SIGN_IN", entry.getStaff(),
                    "Sign-in attempt on a deactivated account",
                    "The password was right, so someone still knows it" + (entry.getIpAddress() != null ? " (from " + entry.getIpAddress() + ")" : "") + ".",
                    entry.getCreatedAt(), entry.getCreatedAt(), 1));
        }
    }

    // Entries with a staff member, grouped by them, oldest first.
    private static Map<Long, List<AuditLog>> byStaff(List<AuditLog> logs) {
        return logs.stream().filter(l -> l.getStaff() != null)
                .collect(Collectors.groupingBy(l -> l.getStaff().getId(), LinkedHashMap::new, Collectors.toList()));
    }

    private static String reasonOf(String description) {
        if (description == null) {
            return "";
        }
        int at = description.indexOf("Reason: ");
        return at >= 0 ? description.substring(at + 8) : description;
    }

    private static Alert alert(String severity, String type, Staff staff, String title, String detail,
                               LocalDateTime first, LocalDateTime last, int count) {
        return new Alert(severity, type,
                staff != null ? staff.getId() : null,
                staff != null ? staff.getStaffNumber() : null,
                staff != null ? staff.getFirstName() + " " + staff.getLastName() : null,
                staff != null ? staff.getRole() : null,
                title, detail, first, last, count);
    }
}
