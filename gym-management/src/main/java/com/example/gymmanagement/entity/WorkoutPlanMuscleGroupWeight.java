package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.MuscleGroup;
import jakarta.persistence.*;
import lombok.*;

/**
 * Hệ số nhân tạ tích lũy theo từng nhóm cơ, gắn theo 1 giáo án (workoutPlan).
 * currentWeightKg (WorkoutPlanExercise) = baseWeightKg * multiplier
 * Mỗi khi tạo giáo án AI mới (planId mới) -> multiplier tự động bắt đầu lại = 1.0
 */
@Entity
@Table(name = "plan_muscle_group_weight",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workout_plan_id", "muscle_group"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutPlanMuscleGroupWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id")
    private WorkoutPlan workoutPlan;

    @Enumerated(EnumType.STRING)
    private MuscleGroup muscleGroup;

    @Builder.Default
    private Double multiplier = 1.0;
}