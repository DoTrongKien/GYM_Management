package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_exercise_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionExerciseLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    private Integer setsCompleted;
    private Integer repsCompleted;
    private Integer durationSeconds;
    private Double weightUsedKg;
    private Boolean isCompleted = false;
    private String notes;
    private LocalDateTime loggedAt = LocalDateTime.now();
}