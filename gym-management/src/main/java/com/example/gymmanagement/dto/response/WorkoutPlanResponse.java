package com.example.gymmanagement.dto.response;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutPlanResponse {
    private Long   id;
    private String planName;
    private String description;
    private Goal   goal;
    private FitnessLevel targetLevel;
    private Integer durationWeeks;
    private Integer sessionsPerWeek;
    private Integer currentWeek;
    private Boolean isActive;
    private Boolean isAiGenerated;
    private Boolean isTemplate;
    private Boolean isCompleted;
    private LocalDate  weekStartDate;
    private LocalDateTime createdAt;

    // Starting metrics
    private Double startingBmi;
    private Double startingWeight;

    // Adjustments applied
    private Integer difficultyAdjustment;
    private Integer setsAdjustment;
    private Integer repsAdjustment;

    private List<WorkoutPlanDayResponse> planDays;

    // Gợi ý ngày tập tối ưu (theo mục tiêu)
    private List<String>             suggestedDays;
    private String                   scheduleNote;

    // Tiến độ tuần hiện tại
    private Integer enrolledThisWeek;
    private Integer completedThisWeek;
}