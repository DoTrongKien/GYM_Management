package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_plan_exercises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutPlanExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_day_id")
    private WorkoutPlanDay planDay;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    private Integer sets;
    private Integer reps;
    private Integer durationSeconds;
    private Integer restSeconds;
    private Integer orderIndex;
    private String notes;

    // ── MỚI: tạ khởi điểm (nhập 1 lần tuần đầu) + tạ hiện tại (auto tính theo tuần) ──
    private Double baseWeightKg;
    private Double currentWeightKg;
    private Integer weightUpdatedWeek; // tuần mà currentWeightKg vừa được cập nhật -> dùng để show "hộp quà bí ẩn"
}