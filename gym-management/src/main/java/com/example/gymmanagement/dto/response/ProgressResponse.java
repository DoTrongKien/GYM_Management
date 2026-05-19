package com.example.gymmanagement.dto.response;
import lombok.*;
import java.time.LocalDate;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProgressResponse {
    private Long id;
    private Double weight;
    private Double height;
    private Double bmi;
    private Double bodyFatPercentage;
    private Double muscleMassKg;
    private Double chestCm;
    private Double waistCm;
    private Double hipCm;
    private Double armCm;
    private Double thighCm;
    private LocalDate recordedDate;
    private String notes;
    private Double weightChange;  // compared to previous
}