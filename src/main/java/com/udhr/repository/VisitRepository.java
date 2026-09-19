package com.udhr.repository;

import com.udhr.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByPatientIdOrderByVisitDateDesc(Long patientId);
    List<Visit> findByStaffId(Long staffId);
    List<Visit> findByFacilityId(Long facilityId);
    List<Visit> findByFacilityIdOrderByVisitDateDesc(Long facilityId);
}
