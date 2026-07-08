package com.example.gymmanagement.dto.request;

import lombok.Data;

@Data
public class ExerciseLogRequest {
    private Long exerciseId;

    // MỚI: thay cho isCompleted boolean cũ
    private Integer completionPercent; // 0/25/50/75/100

    private Double weightUsedKg;
    private String notes;
}