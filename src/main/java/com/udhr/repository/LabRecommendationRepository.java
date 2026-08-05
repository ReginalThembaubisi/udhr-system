package com.udhr.repository;

import com.udhr.model.LabRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LabRecommendationRepository extends JpaRepository<LabRecommendation, Long> {
    List<LabRecommendation> findByAlertId(Long alertId);
    List<LabRecommendation> findByPatientId(Long patientId);
}
