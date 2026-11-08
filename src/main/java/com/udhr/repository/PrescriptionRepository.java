package com.udhr.repository;

import com.udhr.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    // Dispensing locks the prescription row, so two pharmacists pressing
    // "Dispense" at once can't both hand it over (fix list P1-10).
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Prescription p where p.id = :id")
    java.util.Optional<Prescription> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Prescription> findByPatientIdAndActiveTrue(Long patientId);
    List<Prescription> findByDoctorId(Long doctorId);
    List<Prescription> findByPatientIdAndDispenseMethodAndDispensedFalseOrderByCreatedAtDesc(Long patientId, String dispenseMethod);
    List<Prescription> findByFacilityIdAndDispenseMethodAndDispensedFalseOrderByCreatedAtDesc(Long facilityId, String dispenseMethod);
    List<Prescription> findByFacilityIdOrderByCreatedAtDesc(Long facilityId);
}
