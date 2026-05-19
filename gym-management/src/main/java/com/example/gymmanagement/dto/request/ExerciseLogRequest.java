package com.example.gymmanagement.dto.request;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class ExerciseLogRequest {
    private Long exerciseId;
    private Integer setsCompleted;
    private Integer repsCompleted;
    private Integer durationSeconds;
    private Double weightUsedKg;
    private Boolean isCompleted;
    private String notes;
}