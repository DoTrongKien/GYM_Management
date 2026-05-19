package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.Goal;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NutritionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String planName;
    private Integer dailyCalories;
    private Integer proteinGrams;
    private Integer carbsGrams;
    private Integer fatGrams;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    private String mealSuggestions; // JSON string
    private Boolean isAiGenerated = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}