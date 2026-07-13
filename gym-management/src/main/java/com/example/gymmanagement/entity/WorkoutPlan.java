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
    private Integer maxMana;           // = FS * 2, tính khi tạo/rebuild giáo án AI
    private Integer currentMana;       // giá trị runtime, trừ dần mỗi checkout
    private LocalDate lastTrainingDate; // ngày checkout gần nhất, dùng để tính hồi phục

    // ── Chặn applyRegen() cộng mana nhiều lần trong cùng 1 ngày ──
    private LocalDate lastManaRegenDate;

    // ── Lịch tập đã được CHỐT cho giáo án AI (mục 8.3 I.docx) ──
    // Dạng "1,3,5" (ISO dayOfWeek, phân tách bởi dấu phẩy).
    // CHỈ được ghi khi người dùng CHỦ ĐỘNG chọn lại lịch qua API confirm-schedule.
    private String confirmedScheduleDows;

    // ── MỚI: Snapshot Thể lực / Thể trạng tại THỜI ĐIỂM giáo án AI được tạo ──
    // Tính đúng 1 lần trong WorkoutPlanService.generateAIPlanWithGoal(), ngay trước khi
    // save plan. Từ đó về sau KHÔNG tính lại — dù UserProfile của người dùng có đổi,
    // giáo án này vẫn giữ nguyên giá trị lúc tạo. Với giáo án mẫu (template), các field
    // này luôn null vì không gắn với 1 UserProfile cụ thể tại thời điểm tạo.
    private Integer fitnessScore;

    @Enumerated(EnumType.STRING)
    private FitnessCalculator.FsLevel fitnessLevel;

    @Enumerated(EnumType.STRING)
    private FitnessCalculator.BodyType bodyType;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "workoutPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkoutPlanDay> planDays;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (currentWeek == null) currentWeek = 1;
    }
}