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
}