package com.example.gymmanagement.dto.response;
import com.example.gymmanagement.enums.SessionStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutSessionResponse {
    private Long id;
    private LocalDate sessionDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private SessionStatus status;
    private Integer totalCaloriesBurned;
    private Integer durationMinutes;
    private String notes;
    private Integer weekNumber;
    private String planName;
    private String dayName;
    private List<ExerciseLogResponse> exerciseLogs;
}