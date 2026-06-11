package com.example.gymmanagement.dto.request;
import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class UserProfileRequest {
    private Double height;
    private Double weight;
    private Integer age;
    private String gender;
    private Goal goal;
    private FitnessLevel fitnessLevel;
    private Integer availableDaysPerWeek;
    private Integer preferredSessionDuration;
    private String medicalConditions;

    private Double bodyFatPercentage;
}