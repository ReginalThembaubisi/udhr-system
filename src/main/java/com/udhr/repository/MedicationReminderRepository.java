package com.udhr.repository;

import com.udhr.model.MedicationReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicationReminderRepository extends JpaRepository<MedicationReminder, Long> {
    List<MedicationReminder> findByPatientId(Long patientId);
    List<MedicationReminder> findByPatientIdAndIsActiveTrue(Long patientId);
    List<MedicationReminder> findByPrescriptionId(Long prescriptionId);
}
