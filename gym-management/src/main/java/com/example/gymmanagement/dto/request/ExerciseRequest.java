package com.example.gymmanagement.dto.request;

import com.example.gymmanagement.enums.Difficulty;
import com.example.gymmanagement.enums.MuscleGroup;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ExerciseRequest {
    private String name;
    private String description;
    private String videoUrl;
    private String imageUrl;
    private MuscleGroup muscleGroup;
    private Difficulty difficulty;
    private Integer caloriesBurned;
    private Integer defaultSets;
    private Integer defaultReps;
    private Integer defaultDurationSeconds;
    private Integer restSeconds;

    // Benefit scores (0-10)
    private Integer muscleGainScore  = 0;
    private Integer weightLossScore  = 0;
    private Integer enduranceScore   = 0;
    private Integer flexibilityScore = 0;
    private Integer maintenanceScore = 0;

    private Integer staminaCost = 10; // 0-200, mặc định 10
    private Boolean usesWeight;
}