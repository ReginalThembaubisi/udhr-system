package com.udhr.repository;

import com.udhr.model.ClinicalAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicalAlertRepository extends JpaRepository<ClinicalAlert, Long> {
    List<ClinicalAlert> findByIsResolvedFalseOrderByCreatedAtDesc();
    List<ClinicalAlert> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<ClinicalAlert> findByPatientIdAndIsResolvedFalse(Long patientId);
    Optional<ClinicalAlert> findByPatientIdAndAlertTypeAndIsResolvedFalse(Long patientId, ClinicalAlert.AlertType alertType);
}
