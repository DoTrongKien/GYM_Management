package com.example.gymmanagement.dto.request;
import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class WorkoutPlanRequest {
    private String planName;
    private String description;
    private Goal goal;
    private FitnessLevel targetLevel;
    private Integer durationWeeks;
    private Integer sessionsPerWeek;
    private Boolean isActive = true;
}