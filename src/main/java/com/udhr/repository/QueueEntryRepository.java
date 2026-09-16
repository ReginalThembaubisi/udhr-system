package com.udhr.repository;

import com.udhr.model.QueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    List<QueueEntry> findByFacilityIdAndQueueDateAndStatusInOrderByQueueNumberAsc(
            Long facilityId, LocalDate queueDate, List<QueueEntry.Status> statuses);
    List<QueueEntry> findByPatientIdOrderByCheckedInAtDesc(Long patientId);
    long countByFacilityIdAndQueueDate(Long facilityId, LocalDate queueDate);
}
