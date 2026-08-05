package com.udhr.repository;

import com.udhr.model.Symptom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SymptomRepository extends JpaRepository<Symptom, Long> {
    Optional<Symptom> findByInfermedicaId(String infermedicaId);
    Optional<Symptom> findByName(String name);
}
