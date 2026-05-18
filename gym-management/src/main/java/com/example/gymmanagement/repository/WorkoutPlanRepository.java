package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
}
