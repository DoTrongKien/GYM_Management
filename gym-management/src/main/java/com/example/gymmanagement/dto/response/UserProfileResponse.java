package com.example.gymmanagement.dto.response;
import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;

import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private Double height;
    private Double weight;
    private LocalDate dateOfBirth;
    private String gender;
    private Double bmi;
    private String bmiCategory;
    private Goal goal;
    private FitnessLevel fitnessLevel;
    private Integer availableDaysPerWeek;
    private Integer preferredSessionDuration;
    private String medicalConditions;

    private Double bodyFatPercentage;
}