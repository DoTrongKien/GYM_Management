package com.example.gymmanagement.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private String muscleGroup;

    private String difficulty;

    private Integer caloriesBurned;
}
