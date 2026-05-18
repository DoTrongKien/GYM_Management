package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_plan_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutPlanExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workout_plan_id")
    private WorkoutPlan workoutPlan;

    @ManyToOne
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    private Integer sets;

    private Integer reps;

    private String dayOfWeek;
}
