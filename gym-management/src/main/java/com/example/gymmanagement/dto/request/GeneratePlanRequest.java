package com.example.gymmanagement.dto.request;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class GeneratePlanRequest {
    // Tất cả optional - nếu null sẽ lấy từ profile
    private Goal goal;
    private FitnessLevel fitnessLevel;
    private Integer availableDaysPerWeek;
}