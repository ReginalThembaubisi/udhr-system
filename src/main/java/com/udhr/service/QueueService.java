package com.udhr.service;

import com.udhr.dto.QueueCheckInRequest;
import com.udhr.model.Patient;
import com.udhr.model.QueueEntry;
import com.udhr.model.Staff;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.QueueEntryRepository;
import com.udhr.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class QueueService {

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    private static final Map<QueueEntry.Urgency, Integer> URGENCY_PRIORITY = Map.of(
            QueueEntry.Urgency.RED, 0,
            QueueEntry.Urgency.YELLOW, 1,
            QueueEntry.Urgency.GREEN, 2
    );

    @Transactional
    public QueueEntry checkIn(QueueCheckInRequest request, String staffNumber) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        LocalDate today = LocalDate.now();
        long countToday = queueEntryRepository.countByFacilityIdAndQueueDate(staff.getFacility().getId(), today);

        QueueEntry entry = new QueueEntry();
        entry.setPatient(patient);
        entry.setFacility(staff.getFacility());
        entry.setDepartment(QueueEntry.Department.valueOf(request.getDepartment()));
        entry.setReason(request.getReason());
        entry.setUrgency(request.getUrgency() != null && !request.getUrgency().isBlank()
                ? QueueEntry.Urgency.valueOf(request.getUrgency())
                : QueueEntry.Urgency.GREEN);
        entry.setStatus(QueueEntry.Status.WAITING);
        entry.setQueueNumber((int) countToday + 1);
        entry.setCheckedInBy(staff);

        return queueEntryRepository.save(entry);
    }

    public List<QueueEntry> getTodayQueue(String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        List<QueueEntry> entries = queueEntryRepository.findByFacilityIdAndQueueDateAndStatusInOrderByQueueNumberAsc(
                staff.getFacility().getId(), LocalDate.now(),
                List.of(QueueEntry.Status.WAITING, QueueEntry.Status.IN_CONSULTATION));

        entries.sort(Comparator
                .comparing((QueueEntry e) -> URGENCY_PRIORITY.get(e.getUrgency()))
                .thenComparing(QueueEntry::getQueueNumber));

        return entries;
    }

    @Transactional
    public QueueEntry updateUrgency(Long id, String urgency) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        entry.setUrgency(QueueEntry.Urgency.valueOf(urgency));
        return queueEntryRepository.save(entry);
    }

    @Transactional
    public QueueEntry callNext(Long id, String staffNumber) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        entry.setStatus(QueueEntry.Status.IN_CONSULTATION);
        entry.setAttendingStaff(staff);
        entry.setCalledAt(LocalDateTime.now());
        return queueEntryRepository.save(entry);
    }

    @Transactional
    public QueueEntry complete(Long id) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        entry.setStatus(QueueEntry.Status.COMPLETED);
        entry.setCompletedAt(LocalDateTime.now());
        return queueEntryRepository.save(entry);
    }

    @Transactional
    public QueueEntry cancel(Long id) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        entry.setStatus(QueueEntry.Status.CANCELLED);
        return queueEntryRepository.save(entry);
    }

    public List<QueueEntry> getPatientHistory(Long patientId) {
        return queueEntryRepository.findByPatientIdOrderByCheckedInAtDesc(patientId);
    }
}
