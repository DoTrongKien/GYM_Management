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
    private Long planId;  // === dùng để FE match session đúng plan, tránh nhầm plan cùng tên
    private String  dayName;
    private String  customSessionName;
    private Boolean isCustom;
    private Integer completionRate;
    private Boolean isLastSessionOfWeek;
    private String dayMismatchWarning;

    // ── MỚI: mục 8.3 I.docx — khi hệ thống không còn xác định được lịch tập chuẩn nào
    // phù hợp với lịch sử check-in, FE phải hiện popup cho người dùng chọn lại 1 trong
    // các lịch khuyến nghị (scheduleOptions), rồi gọi API /confirm-schedule.
    @Builder.Default
    private Boolean scheduleSelectionRequired = false;
    private List<List<Integer>> scheduleOptions;

    private Double  checkoutWeight;
    private Double  checkoutBodyFat;
    private List<ExerciseLogResponse>         exerciseLogs;
    private List<WorkoutPlanExerciseResponse> planExercises;

    private Boolean injuryRisk; // true nếu buổi tập vừa checkout vượt quá mana hiện có
}