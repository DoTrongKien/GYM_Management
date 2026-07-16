package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import com.example.gymmanagement.service.FitnessCalculator;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "workout_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String planName;
    private String description;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    private FitnessLevel targetLevel;

    private Integer durationWeeks;
    private Integer sessionsPerWeek;
    private Integer currentWeek;

    @Builder.Default
    private Boolean isActive      = true;
    @Builder.Default
    private Boolean isAiGenerated = false;

    @Builder.Default
    private Boolean isTemplate = false;

    @Builder.Default
    private Boolean isCompleted   = false;

    private LocalDate weekStartDate;

    private Double startingBmi;
    private Double startingWeight;

    @Builder.Default
    private Integer difficultyAdjustment = 0;

    @Builder.Default
    private Integer setsAdjustment       = 0;

    @Builder.Default
    private Integer repsAdjustment       = 0;

    @Builder.Default
    private Integer exercisesAdjustment  = 0;

    private String weightAdjustmentNote;

    // ── Hệ thống Mana (thể lực) ─────────────────────────
    private Integer maxMana;
    private Integer currentMana;
    private LocalDate lastTrainingDate;

    private LocalDate lastManaRegenDate;

    // ── Lịch tập đã được CHỐT cho giáo án AI ──
    private String confirmedScheduleDows;

    // ── Snapshot Thể lực / Thể trạng tại THỜI ĐIỂM giáo án AI được tạo ──
    private Integer fitnessScore;

    @Enumerated(EnumType.STRING)
    private FitnessCalculator.FsLevel fitnessLevel;

    @Enumerated(EnumType.STRING)
    private FitnessCalculator.BodyType bodyType;

    // ── Mục tiêu đo lường được của giáo án (Business Rules v2) ──
    private String  targetMetricType;
    private Double  targetBaselineValue;
    private Double  targetGoalValue;
    private Double  targetCurrentValue;
    private Boolean targetAchieved;

    // ── MỚI (Patch 7): thời lượng ước tính ban đầu do AI sinh ra khi tạo giáo án.
    // KHÔNG BAO GIỜ thay đổi sau khi tạo (LOCKED — Business Rules v2 mục 3/IX).
    // durationWeeks vẫn thay đổi runtime theo tiến độ (Patch 8), estimatedWeeks là mẫu số
    // cố định để tính %thời gian đã sử dụng = currentWeek / estimatedWeeks.
    // Giáo án tạo TRƯỚC Patch 7 sẽ có estimatedWeeks = null -> áp dụng logic cũ (LOCKED).
    private Integer estimatedWeeks;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "workoutPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkoutPlanDay> planDays;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (currentWeek == null) currentWeek = 1;
    }
}