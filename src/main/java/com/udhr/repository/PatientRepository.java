package com.udhr.repository;

import com.udhr.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByIdNumber(String idNumber);
    boolean existsByIdNumber(String idNumber);
    Optional<Patient> findByUhid(String uhid);
    boolean existsByUhid(String uhid);
    Optional<Patient> findByPassportNumber(String passportNumber);
}
