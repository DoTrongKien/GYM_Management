package com.example.gymmanagement.dto.request;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class GeneratePlanWithGoalRequest {
    private Goal         goal;
    private FitnessLevel fitnessLevel;
    private Integer      daysPerWeek;

    // ── MỚI (Patch 7): target theo Goal (Business Rules v2) ──
    // MUSCLE_GAIN: bắt buộc, phải > 0. WEIGHT_LOSS: bắt buộc, phải < 0.
    // MAINTENANCE / ENDURANCE: không dùng field này.
    private Double targetDeltaKg;

    // ENDURANCE: bắt buộc cả 2. Giá trị hợp lệ: "PUSHUP_REPS" | "PLANK_SECONDS" | "SQUAT_REPS".
    private String enduranceMetric;
    private Double enduranceTargetValue;
}