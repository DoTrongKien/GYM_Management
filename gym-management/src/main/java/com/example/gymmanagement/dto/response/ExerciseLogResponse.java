package com.example.gymmanagement.dto.response;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExerciseLogResponse {
    private Long id;
    private Long exerciseId;
    private String exerciseName;
    private Integer setsCompleted;
    private Integer repsCompleted;
    private Integer durationSeconds;
    private Double weightUsedKg;
    private Boolean isCompleted;
    private String notes;
}
