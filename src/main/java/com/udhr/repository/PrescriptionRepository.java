package com.udhr.repository;

import com.udhr.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Prescription> findByPatientIdAndActiveTrue(Long patientId);
    List<Prescription> findByDoctorId(Long doctorId);
    List<Prescription> findByPatientIdAndDispenseMethodAndDispensedFalseOrderByCreatedAtDesc(Long patientId, String dispenseMethod);
    List<Prescription> findByFacilityIdAndDispenseMethodAndDispensedFalseOrderByCreatedAtDesc(Long facilityId, String dispenseMethod);
    List<Prescription> findByFacilityIdOrderByCreatedAtDesc(Long facilityId);
}
