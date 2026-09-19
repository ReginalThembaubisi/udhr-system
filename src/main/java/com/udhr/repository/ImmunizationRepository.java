package com.udhr.repository;

import com.udhr.model.Immunization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImmunizationRepository extends JpaRepository<Immunization, Long> {
    List<Immunization> findByPatientIdOrderByScheduledDateAsc(Long patientId);
}
