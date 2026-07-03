package com.example.gymmanagement.dto.response;

import com.example.gymmanagement.enums.SessionStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutSessionResponse {
    private Long   id;
    private LocalDate     sessionDate;
    private LocalTime     scheduledTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private SessionStatus status;
    private Integer totalCaloriesBurned;
    private Integer durationMinutes;
    private String  notes;
    private Integer weekNumber;
    private String  planName;
    private Long planId;  // === MỚI: dùng để FE match session đúng plan, tránh nhầm plan cùng tên
    private String  dayName;
    private String  customSessionName;
    private Boolean isCustom;
    private Integer completionRate;
    private Boolean isLastSessionOfWeek;
    private String dayMismatchWarning;
    private Double  checkoutWeight;
    private Double  checkoutBodyFat;
    private List<ExerciseLogResponse>         exerciseLogs;
    private List<WorkoutPlanExerciseResponse> planExercises;
}