package com.example.gymmanagement.repository;
import com.example.gymmanagement.entity.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    List<WorkoutPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<WorkoutPlan> findByUserIdAndIsActiveTrue(Long userId);
    List<WorkoutPlan> findByIsActiveTrueOrderByCreatedAtDesc();

    List<WorkoutPlan> findByIsTemplateTrueAndIsActiveTrueOrderByCreatedAtDesc();
    List<WorkoutPlan> findByIsTemplateTrueOrderByCreatedAtDesc(); // admin xem cả inactive

}