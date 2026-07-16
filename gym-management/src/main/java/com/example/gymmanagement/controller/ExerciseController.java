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
        List<Exercise> list;
        if (muscleGroup != null && difficulty != null) {
            list = exerciseRepository.findByMuscleGroupAndDifficultyAndIsActiveTrue(
                    MuscleGroup.valueOf(muscleGroup.toUpperCase()),
                    Difficulty.valueOf(difficulty.toUpperCase()));
        } else if (muscleGroup != null) {
            list = exerciseRepository.findByMuscleGroupAndIsActiveTrue(
                    MuscleGroup.valueOf(muscleGroup.toUpperCase()));
        } else if (difficulty != null) {
            list = exerciseRepository.findByDifficultyAndIsActiveTrue(
                    Difficulty.valueOf(difficulty.toUpperCase()));
        } else {
            list = exerciseRepository.findByIsActiveTrue();
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Exercise>> getById(@PathVariable Long id) {
        Exercise ex = exerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));
        return ResponseEntity.ok(ApiResponse.success(ex));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Exercise>> create(@RequestBody ExerciseRequest req) {
        Exercise ex = Exercise.builder()
                .name(req.getName()).description(req.getDescription())
                .videoUrl(req.getVideoUrl()).imageUrl(req.getImageUrl())
                .muscleGroup(req.getMuscleGroup()).difficulty(req.getDifficulty())
                .caloriesBurned(req.getCaloriesBurned())
                .defaultSets(req.getDefaultSets()).defaultReps(req.getDefaultReps())
                .defaultDurationSeconds(req.getDefaultDurationSeconds())
                .restSeconds(req.getRestSeconds())
                .muscleGainScore(req.getMuscleGainScore()   != null ? req.getMuscleGainScore()   : 0)
                .weightLossScore(req.getWeightLossScore()   != null ? req.getWeightLossScore()   : 0)
                .enduranceScore(req.getEnduranceScore()     != null ? req.getEnduranceScore()    : 0)
                .flexibilityScore(req.getFlexibilityScore() != null ? req.getFlexibilityScore() : 0)
                .maintenanceScore(req.getMaintenanceScore() != null ? req.getMaintenanceScore() : 0)
                .staminaCost(req.getStaminaCost() != null ? req.getStaminaCost() : 10)
                .usesWeight(req.getUsesWeight())
                .isActive(true).build();
        return ResponseEntity.ok(ApiResponse.success(exerciseRepository.save(ex), "Đã thêm bài tập!"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Exercise>> update(@PathVariable Long id,
                                                        @RequestBody ExerciseRequest req) {
        Exercise ex = exerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));
        if (req.getName()        != null) ex.setName(req.getName());
        if (req.getDescription() != null) ex.setDescription(req.getDescription());
        if (req.getMuscleGroup() != null) ex.setMuscleGroup(req.getMuscleGroup());
        if (req.getDifficulty()  != null) ex.setDifficulty(req.getDifficulty());
        if (req.getCaloriesBurned()         != null) ex.setCaloriesBurned(req.getCaloriesBurned());
        if (req.getDefaultSets()            != null) ex.setDefaultSets(req.getDefaultSets());
        if (req.getDefaultReps()            != null) ex.setDefaultReps(req.getDefaultReps());
        if (req.getDefaultDurationSeconds() != null) ex.setDefaultDurationSeconds(req.getDefaultDurationSeconds());
        if (req.getRestSeconds()            != null) ex.setRestSeconds(req.getRestSeconds());
        if (req.getVideoUrl()               != null) ex.setVideoUrl(req.getVideoUrl());
        // Cập nhật scores
        if (req.getMuscleGainScore()   != null) ex.setMuscleGainScore(req.getMuscleGainScore());
        if (req.getWeightLossScore()   != null) ex.setWeightLossScore(req.getWeightLossScore());
        if (req.getEnduranceScore()    != null) ex.setEnduranceScore(req.getEnduranceScore());
        if (req.getFlexibilityScore()  != null) ex.setFlexibilityScore(req.getFlexibilityScore());
        if (req.getMaintenanceScore()  != null) ex.setMaintenanceScore(req.getMaintenanceScore());
        if (req.getStaminaCost()       != null) ex.setStaminaCost(req.getStaminaCost());
        if (req.getUsesWeight()        != null) ex.setUsesWeight(req.getUsesWeight());
        return ResponseEntity.ok(ApiResponse.success(exerciseRepository.save(ex), "Đã cập nhật!"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Exercise ex = exerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));
        ex.setIsActive(false);
        exerciseRepository.save(ex);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã ẩn bài tập"));
    }
}