package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.Difficulty;
import com.example.gymmanagement.enums.MuscleGroup;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "exercises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String videoUrl;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private MuscleGroup muscleGroup;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private Integer caloriesBurned;
    private Integer defaultSets;
    private Integer defaultReps;
    private Integer defaultDurationSeconds;
    private Integer restSeconds;

    @Builder.Default
    private Integer muscleGainScore   = 0;

    @Builder.Default
    private Integer weightLossScore   = 0;

    @Builder.Default
    private Integer enduranceScore    = 0;

    @Builder.Default
    private Integer flexibilityScore  = 0;

    @Builder.Default
    private Integer maintenanceScore  = 0;

    @ColumnDefault("10")
    @Builder.Default
    private Integer staminaCost = 10;

    @Builder.Default
    private Boolean isActive = true;

    private Boolean usesWeight;
}