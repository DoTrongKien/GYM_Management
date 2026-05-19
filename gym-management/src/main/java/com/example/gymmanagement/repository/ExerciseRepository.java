package com.example.gymmanagement.repository;
import com.example.gymmanagement.entity.Exercise;
import com.example.gymmanagement.enums.Difficulty;
import com.example.gymmanagement.enums.MuscleGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByIsActiveTrue();
    List<Exercise> findByMuscleGroupAndDifficultyAndIsActiveTrue(MuscleGroup muscleGroup, Difficulty difficulty);
    List<Exercise> findByDifficultyAndIsActiveTrue(Difficulty difficulty);
    List<Exercise> findByMuscleGroupAndIsActiveTrue(MuscleGroup muscleGroup);
}