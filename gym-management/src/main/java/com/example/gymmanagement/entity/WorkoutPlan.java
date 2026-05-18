package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workout_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String planName;

    private String goal;

    private Integer durationWeeks;

    private LocalDateTime createdAt = LocalDateTime.now();
}
