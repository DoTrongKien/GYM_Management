package com.example.gymmanagement.dto.request;

import lombok.Data;

@Data
public class ExerciseLogRequest {
    private Long exerciseId;

    // ── MỚI (Patch 4): thay cho completionPercent client-gửi cũ. Server tự tính
    // completionPercent từ 2 field này + dữ liệu kế hoạch (WorkoutPlanExercise).
    // Không bắt buộc — thiếu thì completionPercent = null (không throw lỗi). ──
    private Integer repsCompleted;
    private Integer durationCompleted;

    private Double weightUsedKg;
    private String notes;
}