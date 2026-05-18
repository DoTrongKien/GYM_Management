package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "workout_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "workout_plan_id")
    private WorkoutPlan workoutPlan;

    private LocalDate sessionDate;

    private String status;

    private String notes;
}
