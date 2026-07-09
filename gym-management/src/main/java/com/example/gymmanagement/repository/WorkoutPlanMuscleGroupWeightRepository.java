package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.WorkoutPlanMuscleGroupWeight;
import com.example.gymmanagement.enums.MuscleGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkoutPlanMuscleGroupWeightRepository extends JpaRepository<WorkoutPlanMuscleGroupWeight, Long> {
    Optional<WorkoutPlanMuscleGroupWeight> findByWorkoutPlanIdAndMuscleGroup(Long planId, MuscleGroup mg);
}