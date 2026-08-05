package com.udhr.repository;

import com.udhr.model.FoodScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FoodScanRepository extends JpaRepository<FoodScan, Long> {
    List<FoodScan> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
