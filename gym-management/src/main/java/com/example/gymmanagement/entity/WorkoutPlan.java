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

    // ── MỚI: Lịch tập đã được CHỐT cho giáo án AI (mục 8.3 I.docx) ──
    // Dạng "1,3,5" (ISO dayOfWeek, phân tách bởi dấu phẩy).
    // CHỈ được ghi khi người dùng CHỦ ĐỘNG chọn lại lịch qua API confirm-schedule,
    // sau khi hệ thống không còn xác định được lịch chuẩn nào phù hợp (survivors == 0).
    // Nếu hệ thống tự xác định được lịch (survivors == 1) thì KHÔNG lưu xuống DB —
    // mỗi lần cần chỉ suy luận lại từ lịch sử check-in (xem WorkoutSessionService).
    // Khi tạo giáo án mới, field này luôn null (builder không set default -> mặc định null).
    private String confirmedScheduleDows;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "workoutPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkoutPlanDay> planDays;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (currentWeek == null) currentWeek = 1;
    }
}