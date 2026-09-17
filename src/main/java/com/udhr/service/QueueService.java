package com.udhr.service;

import com.udhr.dto.PharmacyQueueItem;
import com.udhr.dto.QueueCheckInRequest;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.QueueEntry;
import com.udhr.model.Staff;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.PrescriptionRepository;
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

    @Autowired
    private PrescriptionRepository prescriptionRepository;

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

    // 1-indexed rank of a WAITING entry within its own facility's WAITING
    // queue for that day, ordered the same way getTodayQueue is (urgency,
    // then arrival). Null for any entry that isn't currently WAITING.
    public Integer getWaitingPosition(QueueEntry entry) {
        if (entry.getStatus() != QueueEntry.Status.WAITING) {
            return null;
        }
        List<QueueEntry> waitingToday = queueEntryRepository.findByFacilityIdAndQueueDateAndStatusInOrderByQueueNumberAsc(
                entry.getFacility().getId(), entry.getQueueDate(), List.of(QueueEntry.Status.WAITING));
        waitingToday.sort(Comparator
                .comparing((QueueEntry e) -> URGENCY_PRIORITY.get(e.getUrgency()))
                .thenComparing(QueueEntry::getQueueNumber));

        for (int i = 0; i < waitingToday.size(); i++) {
            if (waitingToday.get(i).getId().equals(entry.getId())) {
                return i + 1;
            }
        }
        return null;
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
    public QueueEntry sendToPharmacy(Long id) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        if (entry.getStatus() != QueueEntry.Status.IN_CONSULTATION) {
            throw new RuntimeException("Only a patient currently in consultation can be sent to pharmacy");
        }
        entry.setStatus(QueueEntry.Status.AWAITING_PHARMACY);
        return queueEntryRepository.save(entry);
    }

    public List<PharmacyQueueItem> getAwaitingPharmacy(String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        List<QueueEntry> entries = queueEntryRepository.findByFacilityIdAndQueueDateAndStatusInOrderByQueueNumberAsc(
                staff.getFacility().getId(), LocalDate.now(), List.of(QueueEntry.Status.AWAITING_PHARMACY));

        return entries.stream()
                .map(entry -> new PharmacyQueueItem(
                        entry,
                        prescriptionRepository.findByPatientIdAndActiveTrue(entry.getPatient().getId())))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public QueueEntry complete(Long id) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));
        entry.setStatus(QueueEntry.Status.COMPLETED);
        entry.setCompletedAt(LocalDateTime.now());
        return queueEntryRepository.save(entry);
    }

    // Best-effort: closes out whatever active queue entry a patient has
    // today at a given facility (e.g. on discharge), so they stop showing
    // as "currently here" once the episode is actually over. A no-op if
    // there's no such entry — most discharges won't have one.
    @Transactional
    public void completeActiveEntryForPatientAtFacility(Long patientId, Long facilityId) {
        LocalDate today = LocalDate.now();
        List<QueueEntry.Status> activeStatuses = List.of(
                QueueEntry.Status.WAITING, QueueEntry.Status.IN_CONSULTATION, QueueEntry.Status.AWAITING_PHARMACY);
        queueEntryRepository.findByPatientIdOrderByCheckedInAtDesc(patientId).stream()
                .filter(qe -> qe.getQueueDate().equals(today)
                        && activeStatuses.contains(qe.getStatus())
                        && qe.getFacility().getId().equals(facilityId))
                .findFirst()
                .ifPresent(qe -> complete(qe.getId()));
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
