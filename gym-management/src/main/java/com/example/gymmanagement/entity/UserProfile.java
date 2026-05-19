package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private Double height; // cm
    private Double weight; // kg
    private Integer age;
    private String gender;
    private Double bmi;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    private FitnessLevel fitnessLevel;

    // Available days per week (1-7)
    private Integer availableDaysPerWeek;

    // Preferred workout time in minutes
    private Integer preferredSessionDuration;

    private String medicalConditions;

    private LocalDate dateOfBirth;
}