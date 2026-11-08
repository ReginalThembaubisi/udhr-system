package com.udhr.service;

import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Emails every active system admin when a HIGH security alert appears (many
 * records opened, records opened outside the list in bulk, an account
 * locked), so someone can end that account's sessions before more is seen.
 *
 * Checked every 5 minutes. Each alert is emailed once a day per account and
 * type; the send is written to the audit trail (SECURITY_ALERT_EMAILED), which
 * is also how repeats are skipped. The email names the staff account only:
 * no patient names, file numbers or reasons, since email is less safe than
 * the system itself. The details are on the Security alerts screen.
 */
@Service
public class SecurityAlertMailer {

    public static final String ACTION = "SECURITY_ALERT_EMAILED";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final SecurityAlertService securityAlertService;
    private final AlertEmailSender sender;
    private final StaffRepository staffRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    @Value("${udhr.site-url:https://udhr-system.onrender.com}")
    private String siteUrl;

    public SecurityAlertMailer(SecurityAlertService securityAlertService, AlertEmailSender sender,
                               StaffRepository staffRepository, AuditLogRepository auditLogRepository,
                               AuditService auditService) {
        this.securityAlertService = securityAlertService;
        this.sender = sender;
        this.staffRepository = staffRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
    }

    @Scheduled(initialDelay = 120_000, fixedDelay = 300_000)
    public void scheduledCheck() {
        // Runs in demo mode too: the emails then land in the demo inbox.
        sendNewAlerts();
    }

    /** Emails each new HIGH alert; returns how many were sent. */
    public int sendNewAlerts() {
        List<String> admins = recipients();
        if (admins.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (SecurityAlertService.Alert alert : securityAlertService.alerts(1)) {
            if (!"HIGH".equals(alert.severity())) {
                continue;
            }
            String key = keyFor(alert);
            if (auditLogRepository.existsByActionAndDescriptionStartingWith(ACTION, key)) {
                continue;
            }
            if (sender.send(admins, subjectFor(alert), bodyFor(alert))) {
                auditService.recordSecurityEvent(null, ACTION,
                        key + " emailed to " + admins.size() + " system admin" + (admins.size() == 1 ? "" : "s")
                                + ": " + alert.title());
                sent++;
            }
        }
        return sent;
    }

    /** Every active system admin's email. */
    public List<String> recipients() {
        return staffRepository.findByRole("SYSTEM_ADMIN").stream()
                .filter(staff -> Boolean.TRUE.equals(staff.getActive()))
                .map(staff -> staff.getEmail())
                .filter(email -> email != null && email.contains("@"))
                .distinct().toList();
    }

    /** A test email from the Security alerts screen; returns how many admins it went to. */
    public int sendTest(String requestedBy) {
        List<String> admins = recipients();
        boolean sent = sender.send(admins, "UDHR security alert: test email",
                "This is a test from the UDHR system, sent by " + requestedBy + ".\n\n"
                        + "Alert emails are working. From now on, high security alerts (many records opened, "
                        + "an account locked after failed sign-ins) are emailed to every system admin.\n\n"
                        + siteUrl + "\n");
        if (sent) {
            auditService.recordSecurityEvent(auditService.currentStaff(), "SECURITY_ALERT_TEST_EMAIL",
                    "sent a test security alert email to " + admins.size() + " system admin" + (admins.size() == 1 ? "" : "s"));
        }
        return sent ? admins.size() : 0;
    }

    // Once a day per account and kind of alert: an alert that keeps growing
    // (more records opened) doesn't send a new email every 5 minutes.
    static String keyFor(SecurityAlertService.Alert alert) {
        return "[" + alert.type() + "/" + (alert.staffId() != null ? alert.staffId() : alert.staffNumber())
                + "/" + LocalDate.now() + "]";
    }

    private static String subjectFor(SecurityAlertService.Alert alert) {
        return "UDHR security alert: " + alert.title()
                + (alert.staffName() != null ? " (" + alert.staffName() + ")" : "");
    }

    private String bodyFor(SecurityAlertService.Alert alert) {
        StringBuilder text = new StringBuilder();
        text.append("The UDHR system raised a high security alert.\n\n");
        text.append("What happened: ").append(alert.title()).append('\n');
        if (alert.staffName() != null || alert.staffNumber() != null) {
            text.append("Account: ").append(alert.staffName() == null ? "" : alert.staffName())
                    .append(alert.staffNumber() == null ? "" : " (" + alert.staffNumber() + ")")
                    .append(alert.role() == null ? "" : ", " + alert.role().replace('_', ' ').toLowerCase())
                    .append('\n');
        }
        text.append("When: ").append(format(alert.firstSeen()));
        if (alert.lastSeen() != null && !alert.lastSeen().equals(alert.firstSeen())) {
            text.append(" to ").append(format(alert.lastSeen()));
        }
        text.append("\n\n");
        text.append("What to do:\n");
        text.append("1. Sign in as a system admin: ").append(siteUrl).append('\n');
        text.append("2. Open Security alerts and press View activity to see what this account did.\n");
        text.append("3. If it wasn't them, press End all sessions, then reset their password.\n\n");
        text.append("Patient details are left out of this email on purpose. They are only in the system.\n");
        return text.toString();
    }

    private static String format(LocalDateTime time) {
        return time == null ? "unknown" : time.format(WHEN);
    }
}
