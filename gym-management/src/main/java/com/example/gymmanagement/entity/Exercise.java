package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.Difficulty;
import com.example.gymmanagement.enums.MuscleGroup;
import jakarta.persistence.*;
import lombok.*;

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

    // ── Chỉ số benefit cho từng mục tiêu (0-10) ─────────────────
    // Điểm càng cao = bài tập càng phù hợp với mục tiêu đó
    @Builder.Default
    private Integer muscleGainScore   = 0;  // điểm tăng cơ

    @Builder.Default
    private Integer weightLossScore   = 0;  // điểm giảm cân

    @Builder.Default
    private Integer enduranceScore    = 0;  // điểm sức bền

    @Builder.Default
    private Integer flexibilityScore  = 0;  // điểm linh hoạt

    @Builder.Default
    private Integer maintenanceScore  = 0;  // điểm duy trì

    @Builder.Default
    private Boolean isActive = true;
}