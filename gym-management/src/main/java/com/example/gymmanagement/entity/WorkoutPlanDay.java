package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "workout_plan_days")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutPlanDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id")
    private WorkoutPlan workoutPlan;

    private Integer dayOfWeek; // 1=Mon, 2=Tue, ..., 7=Sun
    private String dayName;

    @OneToMany(mappedBy = "planDay", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkoutPlanExercise> exercises;
}