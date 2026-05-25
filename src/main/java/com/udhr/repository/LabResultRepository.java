package com.udhr.repository;

import com.udhr.model.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    List<LabResult> findByPatientIdOrderByTestDateDesc(Long patientId);
    List<LabResult> findByStaffId(Long staffId);
}
