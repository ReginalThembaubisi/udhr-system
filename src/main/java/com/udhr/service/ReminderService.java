package com.udhr.service;

import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class ReminderService {

    @Autowired
    private MedicationReminderRepository reminderRepository;

    @Autowired
    private MedicationAdherenceRepository adherenceRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Transactional
    public void createRemindersForPrescription(Prescription prescription) {
        if (prescription == null || prescription.getFrequency() == null) {
            return;
        }

        // Clean existing reminders if any for this prescription to avoid duplication
        List<MedicationReminder> existing = reminderRepository.findByPrescriptionId(prescription.getId());
        if (!existing.isEmpty()) {
            reminderRepository.deleteAll(existing);
        }

        String frequency = prescription.getFrequency().toLowerCase().trim();
        List<String> times = new ArrayList<>();

        if (frequency.contains("once daily")) {
            times.add("08:00");
        } else if (frequency.contains("twice daily")) {
            times.add("08:00");
            times.add("20:00");
        } else if (frequency.contains("three times daily")) {
            times.add("08:00");
            times.add("13:00");
            times.add("20:00");
        } else if (frequency.contains("with meals")) {
            times.add("07:30");
            times.add("12:30");
            times.add("18:30");
        } else {
            // Default fallback: morning dose
            times.add("08:00");
        }

        for (String time : times) {
            MedicationReminder reminder = new MedicationReminder();
            reminder.setPatient(prescription.getPatient());
            reminder.setPrescription(prescription);
            reminder.setReminderTime(time);
            reminder.setFrequency(prescription.getFrequency());
            reminder.setIsActive(true);
            reminderRepository.save(reminder);
        }
        System.out.println("Created " + times.size() + " reminder times for prescription: " + prescription.getMedication());
    }

    @Transactional
    public List<MedicationAdherence> getTodayAdherenceLogsForPatient(String idNumber) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        LocalDate today = LocalDate.now();
        // Dynamically ensure today's logs exist
        generateAdherenceLogs(patient, today, today);

        // Fetch logs for today (from start of today 00:00 to end of today 23:59)
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        return adherenceRepository.findByPatientIdAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                patient.getId(), start, end);
    }

    @Transactional
    public List<MedicationAdherence> getWeeklyAdherenceLogsForPatient(Long patientId) {
        LocalDate today = LocalDate.now();
        LocalDate startWeek = today.minusDays(7); // Last 7 days

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Make sure logs exist
        generateAdherenceLogs(patient, startWeek, today);

        return adherenceRepository.findByPatientIdAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                patientId, startWeek.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    @Transactional
    public void generateAdherenceLogs(Patient patient, LocalDate startDate, LocalDate endDate) {
        List<MedicationReminder> activeReminders = reminderRepository.findByPatientIdAndIsActiveTrue(patient.getId());

        for (MedicationReminder reminder : activeReminders) {
            Prescription prescription = reminder.getPrescription();
            
            // Check if prescription is still active
            if (prescription == null || !prescription.getActive()) {
                continue;
            }

            LocalTime reminderTime = LocalTime.parse(reminder.getReminderTime());

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                // Ensure date is within prescription range
                if (date.isBefore(prescription.getStartDate()) || date.isAfter(prescription.getEndDate())) {
                    continue;
                }

                LocalDateTime scheduledTime = date.atTime(reminderTime);
                
                // Only generate for past or current times
                if (scheduledTime.isAfter(LocalDateTime.now().plusHours(24))) {
                    continue;
                }

                Optional<MedicationAdherence> existing = adherenceRepository
                        .findByReminderIdAndScheduledTime(reminder.getId(), scheduledTime);

                if (existing.isEmpty()) {
                    MedicationAdherence log = new MedicationAdherence();
                    log.setReminder(reminder);
                    log.setPatient(patient);
                    log.setScheduledTime(scheduledTime);
                    log.setStatus("PENDING");
                    adherenceRepository.save(log);
                }
            }
        }
    }

    @Transactional
    public MedicationAdherence updateAdherenceStatus(Long adherenceId, String status, String notes, String requestingPatientIdNumber) {
        MedicationAdherence log = adherenceRepository.findById(adherenceId)
                .orElseThrow(() -> new RuntimeException("Adherence record not found"));

        // A patient may only update their own medication adherence records --
        // adherence IDs are sequential, so this was otherwise a straight IDOR
        // letting one patient forge or erase another's clinical adherence history.
        String ownerIdNumber = log.getPatient() != null ? log.getPatient().getIdNumber() : null;
        if (ownerIdNumber == null || !ownerIdNumber.equals(requestingPatientIdNumber)) {
            throw new AccessDeniedException("This medication reminder does not belong to you.");
        }

        log.setStatus(status.toUpperCase());
        if ("TAKEN".equalsIgnoreCase(status)) {
            log.setTakenAt(LocalDateTime.now());
        } else {
            log.setTakenAt(null);
        }
        log.setNotes(notes);

        return adherenceRepository.save(log);
    }

    public Map<String, Object> getAdherenceStats(Long patientId) {
        List<MedicationAdherence> logs = getWeeklyAdherenceLogsForPatient(patientId);

        int total = 0;
        int taken = 0;
        int missed = 0;
        int pending = 0;

        for (MedicationAdherence log : logs) {
            // Do not count future pending doses
            if ("PENDING".equals(log.getStatus()) && log.getScheduledTime().isAfter(LocalDateTime.now())) {
                continue;
            }
            
            total++;
            if ("TAKEN".equals(log.getStatus())) {
                taken++;
            } else if ("MISSED".equals(log.getStatus())) {
                missed++;
            } else {
                pending++;
            }
        }

        int score = total > 0 ? (taken * 100) / total : 100;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDoses", total);
        stats.put("takenDoses", taken);
        stats.put("missedDoses", missed);
        stats.put("pendingDoses", pending);
        stats.put("adherenceScore", score);

        return stats;
    }
}
