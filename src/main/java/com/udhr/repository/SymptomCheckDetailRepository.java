package com.udhr.repository;

import com.udhr.model.SymptomCheckDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SymptomCheckDetailRepository extends JpaRepository<SymptomCheckDetail, Long> {
    List<SymptomCheckDetail> findBySymptomCheckId(Long symptomCheckId);
}
