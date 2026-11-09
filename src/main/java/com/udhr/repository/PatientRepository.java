package com.udhr.repository;

import com.udhr.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * The identifier look-ups are written out as "=" queries on purpose. Derived
 * from the method name, findByPassportNumber(null) means "where passport is
 * null": a missing identifier then matched every patient registered without a
 * passport (nearly everyone), so a request without an ID number either failed
 * with "2 results were returned" or, with one such patient, quietly used the
 * wrong person's record. "= null" matches nothing, as a missing ID should.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Patient> {
    @Query("select p from Patient p where p.idNumber = :idNumber")
    Optional<Patient> findByIdNumber(@Param("idNumber") String idNumber);

    @Query("select count(p) > 0 from Patient p where p.idNumber = :idNumber")
    boolean existsByIdNumber(@Param("idNumber") String idNumber);

    @Query("select p from Patient p where p.mrn = :mrn")
    Optional<Patient> findByMrn(@Param("mrn") String mrn);

    /**
     * The patient a portal session belongs to. A session is keyed on the SA ID
     * number when the patient has one, otherwise on their file number (MRN),
     * so patients without an SA ID (foreign nationals, passport holders) can
     * use the portal too.
     */
    default Optional<Patient> findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return findByIdNumber(sessionId).or(() -> findByMrn(sessionId));
    }

    @Query("select count(p) > 0 from Patient p where p.mrn = :mrn")
    boolean existsByMrn(@Param("mrn") String mrn);

    @Query("select p from Patient p where p.passportNumber = :passportNumber")
    Optional<Patient> findByPassportNumber(@Param("passportNumber") String passportNumber);

    java.util.List<Patient> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(String firstName, String lastName, java.time.LocalDate dateOfBirth);
}
