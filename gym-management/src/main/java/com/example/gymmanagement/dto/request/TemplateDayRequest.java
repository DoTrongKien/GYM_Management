package com.example.gymmanagement.dto.request;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class TemplateDayRequest {
    private Integer dayOfWeek;  // 1=Mon ... 7=Sun
    private String dayName;
    private List<TemplateExerciseRequest> exercises;
}
