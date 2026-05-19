package com.example.gymmanagement.repository;
import com.example.gymmanagement.entity.NutritionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface NutritionPlanRepository extends JpaRepository<NutritionPlan, Long> {
    List<NutritionPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<NutritionPlan> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}