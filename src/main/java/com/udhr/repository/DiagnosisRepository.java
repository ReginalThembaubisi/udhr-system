package com.udhr.repository;

import com.udhr.model.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    List<Diagnosis> findByPatientIdOrderByDiagnosedAtDesc(Long patientId);
    List<Diagnosis> findByDoctorId(Long doctorId);
}
