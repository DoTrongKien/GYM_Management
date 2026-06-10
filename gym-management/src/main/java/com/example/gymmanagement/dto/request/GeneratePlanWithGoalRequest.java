package com.example.gymmanagement.dto.request;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class GeneratePlanWithGoalRequest {
    private Goal         goal;             // bắt buộc
    private FitnessLevel fitnessLevel;     // tuỳ chọn, lấy từ profile nếu null
    private Integer      daysPerWeek;      // min2-max6, lấy từ profile nếu null
}