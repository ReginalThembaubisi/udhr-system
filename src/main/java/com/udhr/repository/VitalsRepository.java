package com.udhr.repository;

import com.udhr.model.Vitals;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VitalsRepository extends JpaRepository<Vitals, Long> {
    List<Vitals> findByPatientIdOrderByRecordedAtDesc(Long patientId);
    List<Vitals> findByQueueEntryId(Long queueEntryId);
}
