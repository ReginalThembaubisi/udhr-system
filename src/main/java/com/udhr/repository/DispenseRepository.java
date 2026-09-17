package com.udhr.repository;

import com.udhr.model.Dispense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DispenseRepository extends JpaRepository<Dispense, Long> {
    List<Dispense> findByPatientIdOrderByDispensedAtDesc(Long patientId);
    List<Dispense> findByPrescriptionIdOrderByDispensedAtDesc(Long prescriptionId);
    List<Dispense> findByFacilityIdOrderByDispensedAtDesc(Long facilityId);
}
