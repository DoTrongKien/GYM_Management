package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "workout_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Template reference (null nếu custom session)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id")
    private WorkoutPlan workoutPlan;

    // Ngày mẫu user chọn đăng ký (null nếu custom)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_day_id")
    private WorkoutPlanDay planDay;

    // Ngày & giờ user tự chọn
    private LocalDate sessionDate;
    private LocalTime scheduledTime;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;

    private Integer totalCaloriesBurned;
    private Integer durationMinutes;
    private String notes;

    // Tuần tập (1,2,3,...) - user tự chọn khi đăng ký
    private Integer weekNumber;

    // Session tự do không theo plan
    private String customSessionName;

    @Builder.Default
    private Boolean isCustom = false;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SessionExerciseLog> exerciseLogs;

    @PrePersist
    protected void prePersist() {
        if (status == null)   status   = SessionStatus.SCHEDULED;
        if (isCustom == null) isCustom = false;
    }
}