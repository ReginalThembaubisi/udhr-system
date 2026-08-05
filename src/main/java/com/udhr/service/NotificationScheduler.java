package com.udhr.service;

import com.udhr.model.MedicationReminder;
import com.udhr.model.Patient;
import com.udhr.repository.MedicationReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NotificationScheduler {

    @Autowired
    private MedicationReminderRepository reminderRepository;

    @Autowired
    private AfricasTalkingService africasTalkingService;

    // Runs every minute to dispatch due reminders
    @Scheduled(cron = "0 * * * * *")
    public void checkAndDispatchReminders() {
        LocalTime now = LocalTime.now();
        String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        System.out.println("[NotificationScheduler] Checking medication reminders for time: " + timeStr);

        List<MedicationReminder> allReminders = reminderRepository.findAll();
        
        for (MedicationReminder reminder : allReminders) {
            if (reminder.getIsActive() && timeStr.equals(reminder.getReminderTime())) {
                Patient patient = reminder.getPatient();
                if (patient == null) continue;

                String message = String.format("🔔 UDHR Medication Reminder: Hi %s, it's time to take your prescribed medication: %s (%s). Frequency: %s. Scheduled for %s.",
                        patient.getFirstName(),
                        reminder.getPrescription().getMedication(),
                        reminder.getPrescription().getDosage(),
                        reminder.getFrequency(),
                        reminder.getReminderTime());

                // 1. Send SMS
                if (patient.getContactNumber() != null && !patient.getContactNumber().isEmpty()) {
                    africasTalkingService.sendSMS(patient.getContactNumber(), message);
                }

                // 2. Send Email (Mocked Console Dispatch)
                if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                    System.out.println(String.format("[Email Dispatcher] Sending email reminder to: %s | Subject: Medication Reminder - %s | Body: %s",
                            patient.getEmail(),
                            reminder.getPrescription().getMedication(),
                            message));
                }
            }
        }
    }
}
