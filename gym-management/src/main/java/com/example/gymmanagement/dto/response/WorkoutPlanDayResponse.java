package com.example.gymmanagement.dto.response;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutPlanDayResponse {
    private Long id;
    private Integer dayOfWeek;
    private String dayName;
    private List<WorkoutPlanExerciseResponse> exercises;
}