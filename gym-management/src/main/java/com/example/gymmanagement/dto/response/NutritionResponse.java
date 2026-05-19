package com.example.gymmanagement.dto.response;
import com.example.gymmanagement.enums.Goal;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NutritionResponse {
    private Long id;
    private String planName;
    private Integer dailyCalories;
    private Integer proteinGrams;
    private Integer carbsGrams;
    private Integer fatGrams;
    private Goal goal;
    private String mealSuggestions;
    private Boolean isAiGenerated;
    private LocalDateTime createdAt;
}