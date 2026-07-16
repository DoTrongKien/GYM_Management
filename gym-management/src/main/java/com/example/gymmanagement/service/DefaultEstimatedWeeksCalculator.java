package com.example.gymmanagement.service;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import org.springframework.stereotype.Component;

/**
 * Patch 7 — Implementation TẠM THỜI của EstimatedWeeksCalculator.
 * Business Rules v2 chưa chốt công thức tính estimatedWeeks ("chưa cần thiết kế công thức
 * ở giai đoạn này"). Để hệ thống hoạt động được, class này trả về giá trị placeholder cố
 * định = 6 (giữ nguyên hành vi durationWeeks mặc định trước Patch 7).
 *
 * KHI CÓ CÔNG THỨC CHÍNH THỨC: chỉ cần sửa DUY NHẤT class này, không đụng
 * WorkoutPlanService hay bất kỳ nơi nào khác gọi tới interface EstimatedWeeksCalculator.
 */
@Component
public class DefaultEstimatedWeeksCalculator implements EstimatedWeeksCalculator {

    private static final int PLACEHOLDER_WEEKS = 6;

    @Override
    public int calculate(Goal goal, FitnessLevel level, Double targetDeltaKg,
                         Double enduranceBaseline, Double enduranceGoal) {
        return PLACEHOLDER_WEEKS;
    }
}