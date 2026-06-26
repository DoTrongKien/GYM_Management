package com.example.gymmanagement.dto.request;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class TemplateExerciseRequest {
    private Long exerciseId;     // required
    private Integer sets;
    private Integer reps;            // null nếu dùng durationSeconds
    private Integer durationSeconds; // null nếu dùng reps
    private Integer restSeconds;
    private Integer orderIndex;
    private String notes;
}
