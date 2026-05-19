package com.example.gymmanagement.dto.request;
import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class GeneratePlanRequest {
    private Goal goal;
    private FitnessLevel fitnessLevel;
    private Integer availableDaysPerWeek;
    private Integer preferredSessionDuration;
    private String medicalConditions;
    private Double weight;
    private Double height;
    private Integer age;
    private String gender;
}