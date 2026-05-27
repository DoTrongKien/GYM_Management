package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.Exercise;
import com.example.gymmanagement.enums.Difficulty;
import com.example.gymmanagement.enums.MuscleGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByIsActiveTrue();

    List<Exercise> findByMuscleGroupAndIsActiveTrue(MuscleGroup muscleGroup);

    List<Exercise> findByDifficultyAndIsActiveTrue(Difficulty difficulty);

    List<Exercise> findByMuscleGroupAndDifficultyAndIsActiveTrue(MuscleGroup muscleGroup, Difficulty difficulty);

    // Lấy bài tập theo nhóm cơ, sắp xếp theo điểm mục tiêu GIẢM DẦN
    @Query("SELECT e FROM Exercise e WHERE e.muscleGroup = :mg AND e.isActive = true ORDER BY e.muscleGainScore DESC")
    List<Exercise> findByMuscleGroupOrderByMuscleGain(@Param("mg") MuscleGroup mg);

    @Query("SELECT e FROM Exercise e WHERE e.muscleGroup = :mg AND e.isActive = true ORDER BY e.weightLossScore DESC")
    List<Exercise> findByMuscleGroupOrderByWeightLoss(@Param("mg") MuscleGroup mg);

    @Query("SELECT e FROM Exercise e WHERE e.muscleGroup = :mg AND e.isActive = true ORDER BY e.enduranceScore DESC")
    List<Exercise> findByMuscleGroupOrderByEndurance(@Param("mg") MuscleGroup mg);

    @Query("SELECT e FROM Exercise e WHERE e.muscleGroup = :mg AND e.isActive = true ORDER BY e.flexibilityScore DESC")
    List<Exercise> findByMuscleGroupOrderByFlexibility(@Param("mg") MuscleGroup mg);

    @Query("SELECT e FROM Exercise e WHERE e.muscleGroup = :mg AND e.isActive = true ORDER BY e.maintenanceScore DESC")
    List<Exercise> findByMuscleGroupOrderByMaintenance(@Param("mg") MuscleGroup mg);

    // Lấy tất cả bài tập active sắp xếp theo từng điểm
    @Query("SELECT e FROM Exercise e WHERE e.isActive = true ORDER BY e.muscleGainScore DESC")
    List<Exercise> findAllOrderByMuscleGain();

    @Query("SELECT e FROM Exercise e WHERE e.isActive = true ORDER BY e.weightLossScore DESC")
    List<Exercise> findAllOrderByWeightLoss();

    @Query("SELECT e FROM Exercise e WHERE e.isActive = true ORDER BY e.enduranceScore DESC")
    List<Exercise> findAllOrderByEndurance();
}