package com.example.gymmanagement.service;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;

/**
 * Patch 7 — Module độc lập chịu trách nhiệm tính estimatedWeeks ban đầu khi AI sinh giáo án.
 * Business Rules v2 (LOCKED): "Chưa cần thiết kế công thức ở giai đoạn này. AI chịu trách
 * nhiệm sinh estimatedWeeks ban đầu." Interface được tách riêng để sau này thay đổi thuật
 * toán mà KHÔNG cần sửa WorkoutPlanService.
 */
public interface EstimatedWeeksCalculator {
    /**
     * @param targetDeltaKg      dùng cho MUSCLE_GAIN/WEIGHT_LOSS, null với Goal khác
     * @param enduranceBaseline  dùng cho ENDURANCE, null với Goal khác
     * @param enduranceGoal      dùng cho ENDURANCE, null với Goal khác
     */
    int calculate(Goal goal, FitnessLevel level, Double targetDeltaKg,
                  Double enduranceBaseline, Double enduranceGoal);
}