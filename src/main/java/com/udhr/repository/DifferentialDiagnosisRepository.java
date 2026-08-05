package com.udhr.repository;

import com.udhr.model.DifferentialDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DifferentialDiagnosisRepository extends JpaRepository<DifferentialDiagnosis, Long> {
    List<DifferentialDiagnosis> findByAlertId(Long alertId);
    List<DifferentialDiagnosis> findByPatientId(Long patientId);
}
