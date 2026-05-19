package com.example.gymmanagement.repository;
import com.example.gymmanagement.entity.WorkoutPlanDay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WorkoutPlanDayRepository extends JpaRepository<WorkoutPlanDay, Long> {
    List<WorkoutPlanDay> findByWorkoutPlanIdOrderByDayOfWeek(Long planId);
}