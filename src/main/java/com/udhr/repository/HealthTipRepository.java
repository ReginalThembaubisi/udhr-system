package com.udhr.repository;

import com.udhr.model.HealthTip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HealthTipRepository extends JpaRepository<HealthTip, Long> {
    List<HealthTip> findByConditionNameIn(List<String> conditionNames);
}
