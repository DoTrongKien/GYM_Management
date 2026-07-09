package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.WorkoutPlanExercise;
import com.example.gymmanagement.enums.MuscleGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutPlanExerciseRepository extends JpaRepository<WorkoutPlanExercise, Long> {

    // Dùng để cập nhật currentWeightKg cho mọi bài tập cùng nhóm cơ trong 1 giáo án
    List<WorkoutPlanExercise> findByPlanDay_WorkoutPlan_IdAndExercise_MuscleGroup(Long planId, MuscleGroup mg);
}