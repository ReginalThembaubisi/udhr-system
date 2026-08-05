package com.udhr.repository;

import com.udhr.model.SymptomCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SymptomCheckRepository extends JpaRepository<SymptomCheck, Long> {
    List<SymptomCheck> findByPatientIdOrderByCheckedAtDesc(Long patientId);
}
