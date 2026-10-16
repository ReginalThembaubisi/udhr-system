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

                String message = reminderText(patient.getFirstName(), reminder.getReminderTime());

                // Only patients who agreed to SMS reminders get them.
                if (Boolean.TRUE.equals(patient.getSmsRemindersConsent())
                        && patient.getContactNumber() != null && !patient.getContactNumber().isEmpty()) {
                    africasTalkingService.sendSMS(patient.getContactNumber(), message);
                }
            }
        }
    }

    /**
     * No medicine name: an SMS can be read by anyone holding the phone, and
     * some medicines (ARVs, for example) reveal a diagnosis. POPIA treats
     * health information as special personal information (section 32).
     */
    static String reminderText(String firstName, String reminderTime) {
        return String.format("UDHR: Hi %s, it's time for your %s medication. Open the UDHR app to see which one.",
                firstName, reminderTime);
    }
}
