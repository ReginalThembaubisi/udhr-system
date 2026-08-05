package com.udhr.repository;

import com.udhr.model.DietaryGuideline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DietaryGuidelineRepository extends JpaRepository<DietaryGuideline, Long> {
    List<DietaryGuideline> findByConditionNameIn(List<String> conditionNames);
}
