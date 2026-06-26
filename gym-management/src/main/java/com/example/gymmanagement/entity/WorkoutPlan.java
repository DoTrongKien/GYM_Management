package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
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

    private Integer durationWeeks;        // ban đầu = 6
    private Integer sessionsPerWeek;      // min 4 nếu MUSCLE_GAIN/WEIGHT_LOSS
    private Integer currentWeek;          // tuần hiện tại (bắt đầu = 1)

    @Builder.Default
    private Boolean isActive      = true;
    @Builder.Default
    private Boolean isAiGenerated = false;

    // === MỚI: true = template do admin tạo tay (chưa gán user) ===
    @Builder.Default
    private Boolean isTemplate = false;

    @Builder.Default
    private Boolean isCompleted   = false;

    // Ngày bắt đầu chu kỳ tuần đầu tiên
    private LocalDate weekStartDate;

    // BMI / cân nặng khi tạo giáo án (để so sánh tiến độ)
    private Double startingBmi;
    private Double startingWeight;

    // Điều chỉnh tự động theo tiến độ
    @Builder.Default
    private Integer difficultyAdjustment = 0; // -1=giảm, 0=giữ, 1=tăng

    @Builder.Default
    private Integer setsAdjustment       = 0;

    @Builder.Default
    private Integer repsAdjustment       = 0;

    // === TRƯỜNG MỚI: Điều chỉnh số lượng bài tập ===
    @Builder.Default
    private Integer exercisesAdjustment  = 0;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "workoutPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkoutPlanDay> planDays;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (currentWeek == null) currentWeek = 1;
    }
}