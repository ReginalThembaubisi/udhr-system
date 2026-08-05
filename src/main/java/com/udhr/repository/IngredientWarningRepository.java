package com.udhr.repository;

import com.udhr.model.IngredientWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IngredientWarningRepository extends JpaRepository<IngredientWarning, Long> {
    List<IngredientWarning> findByConditionNameIn(List<String> conditionNames);
    List<IngredientWarning> findByIngredientIdAndConditionNameIn(Long ingredientId, List<String> conditionNames);
}
