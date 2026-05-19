package com.example.gymmanagement.dto.response;
import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutPlanResponse {
    private Long id;
    private String planName;
    private String description;
    private Goal goal;
    private FitnessLevel targetLevel;
    private Integer durationWeeks;
    private Integer sessionsPerWeek;
    private Boolean isActive;
    private Boolean isAiGenerated;
    private LocalDateTime createdAt;
    private List<WorkoutPlanDayResponse> planDays;
}