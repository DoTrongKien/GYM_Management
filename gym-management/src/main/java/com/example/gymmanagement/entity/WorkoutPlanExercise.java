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

    // ── MỚI (Patch 3): Mức tạ khuyến nghị — SNAPSHOT, tính ĐÚNG 1 LẦN khi sinh giáo án
    // AI (trong WorkoutPlanService.buildExercisesNew()), KHÔNG cập nhật lại theo tuần,
    // KHÔNG bị ảnh hưởng khi baseWeightKg/currentWeightKg thay đổi, KHÔNG bị ảnh hưởng
    // khi UserProfile của người dùng thay đổi sau này. Độc lập hoàn toàn với
    // baseWeightKg/currentWeightKg. null nếu bài tập không dùng tạ, hoặc thiếu dữ liệu
    // cần thiết để tính (xem điều kiện tính toán trong WorkoutPlanService).
    private Double recommendedWeightKg;
}