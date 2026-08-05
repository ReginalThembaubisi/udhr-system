package com.udhr.repository;

import com.udhr.model.MedicationAdherence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationAdherenceRepository extends JpaRepository<MedicationAdherence, Long> {
    List<MedicationAdherence> findByPatientId(Long patientId);
    List<MedicationAdherence> findByPatientIdAndScheduledTimeBetweenOrderByScheduledTimeAsc(
            Long patientId, LocalDateTime start, LocalDateTime end);
    Optional<MedicationAdherence> findByReminderIdAndScheduledTime(Long reminderId, LocalDateTime scheduledTime);
}
