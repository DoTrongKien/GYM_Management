package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.ExerciseRequest;
import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.entity.Exercise;
import com.example.gymmanagement.enums.Difficulty;
import com.example.gymmanagement.enums.MuscleGroup;
import com.example.gymmanagement.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseRepository exerciseRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Exercise>>> getAll(
            @RequestParam(required = false) String muscleGroup,
            @RequestParam(required = false) String difficulty) {
        List<Exercise> exercises;
        if (muscleGroup != null && difficulty != null) {
            exercises = exerciseRepository.findByMuscleGroupAndDifficultyAndIsActiveTrue(
                    MuscleGroup.valueOf(muscleGroup.toUpperCase()),
                    Difficulty.valueOf(difficulty.toUpperCase()));
        } else if (muscleGroup != null) {
            exercises = exerciseRepository.findByMuscleGroupAndIsActiveTrue(MuscleGroup.valueOf(muscleGroup.toUpperCase()));
        } else if (difficulty != null) {
            exercises = exerciseRepository.findByDifficultyAndIsActiveTrue(Difficulty.valueOf(difficulty.toUpperCase()));
        } else {
            exercises = exerciseRepository.findByIsActiveTrue();
        }
        return ResponseEntity.ok(ApiResponse.success(exercises));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Exercise>> getById(@PathVariable Long id) {
        Exercise ex = exerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));
        return ResponseEntity.ok(ApiResponse.success(ex));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Exercise>> create(@RequestBody ExerciseRequest request) {
        Exercise ex = Exercise.builder()
                .name(request.getName()).description(request.getDescription())
                .videoUrl(request.getVideoUrl()).imageUrl(request.getImageUrl())
                .muscleGroup(request.getMuscleGroup()).difficulty(request.getDifficulty())
                .caloriesBurned(request.getCaloriesBurned())
                .defaultSets(request.getDefaultSets()).defaultReps(request.getDefaultReps())
                .defaultDurationSeconds(request.getDefaultDurationSeconds()).isActive(true)
                .build();
        return ResponseEntity.ok(ApiResponse.success(exerciseRepository.save(ex), "Exercise created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Exercise>> update(@PathVariable Long id, @RequestBody ExerciseRequest request) {
        Exercise ex = exerciseRepository.findById(id).orElseThrow(() -> new RuntimeException("Exercise not found"));
        if (request.getName() != null) ex.setName(request.getName());
        if (request.getDescription() != null) ex.setDescription(request.getDescription());
        if (request.getMuscleGroup() != null) ex.setMuscleGroup(request.getMuscleGroup());
        if (request.getDifficulty() != null) ex.setDifficulty(request.getDifficulty());
        if (request.getCaloriesBurned() != null) ex.setCaloriesBurned(request.getCaloriesBurned());
        if (request.getDefaultSets() != null) ex.setDefaultSets(request.getDefaultSets());
        if (request.getDefaultReps() != null) ex.setDefaultReps(request.getDefaultReps());
        if (request.getVideoUrl() != null) ex.setVideoUrl(request.getVideoUrl());
        return ResponseEntity.ok(ApiResponse.success(exerciseRepository.save(ex), "Exercise updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Exercise ex = exerciseRepository.findById(id).orElseThrow(() -> new RuntimeException("Exercise not found"));
        ex.setIsActive(false);
        exerciseRepository.save(ex);
        return ResponseEntity.ok(ApiResponse.success(null, "Exercise deactivated"));
    }
}