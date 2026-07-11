package com.example.gymmanagement.dto.response;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    // ── SỬA: giờ trả về TẤT CẢ lịch tập khuyến nghị (mỗi phần tử là 1 danh sách ISO
    // dayOfWeek), không còn 1 lịch cố định theo tên ngày tiếng Anh như trước ──
    private List<List<Integer>>      suggestedDays;
    private String                   scheduleNote;

    private String weightAdjustmentNote;

    // Tiến độ tuần hiện tại
    private Integer enrolledThisWeek;
    private Integer completedThisWeek;

    private Integer maxMana;
    private Integer currentMana;
    private String manaMessage;
}