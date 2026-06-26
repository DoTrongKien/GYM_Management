package com.example.gymmanagement.dto.request;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class WorkoutTemplateRequest {
    private String planName;
    private String description;
    private Goal goal;
    private FitnessLevel targetLevel;
    private Integer durationWeeks;
    private Integer sessionsPerWeek;   // = days.size(), FE tự đồng bộ
    private List<TemplateDayRequest> days;
}
