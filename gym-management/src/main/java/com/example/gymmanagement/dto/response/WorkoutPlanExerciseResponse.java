package com.example.gymmanagement.dto.response;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutPlanExerciseResponse {
    private Long id;
    private Long exerciseId;
    private String exerciseName;
    private String muscleGroup;
    private String difficulty;
    private Integer sets;
    private Integer reps;
    private Integer durationSeconds;
    private Integer restSeconds;
    private Integer orderIndex;
    private String notes;
    private String videoUrl;
    private Integer caloriesBurned;

    // ── MỚI ──
    private Double baseWeightKg;
    private Double currentWeightKg;
    private Boolean weightJustRevealed; // true nếu tạ vừa được cập nhật trong tuần hiện tại -> FE show hộp quà bí ẩn

    // ── MỚI (Patch 3): Mức tạ khuyến nghị — snapshot, map thẳng từ entity, không tính lại ──
    private Double recommendedWeightKg;
}