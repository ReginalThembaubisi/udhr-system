package com.udhr.repository;

import com.udhr.model.Vitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VitalsRepository extends JpaRepository<Vitals, Long> {
    List<Vitals> findByPatientIdOrderByRecordedAtDesc(Long patientId);
    Optional<Vitals> findFirstByVisitIdOrderByRecordedAtDesc(Long visitId);
    List<Vitals> findByVisitIdOrderByRecordedAtDesc(Long visitId);
}
