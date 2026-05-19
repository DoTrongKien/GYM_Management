package com.example.gymmanagement.dto.request;
import lombok.*;
import java.time.LocalDate;
@Data @NoArgsConstructor @AllArgsConstructor
public class ProgressRequest {
    private Double weight;
    private Double height;
    private Double bodyFatPercentage;
    private Double muscleMassKg;
    private Double chestCm;
    private Double waistCm;
    private Double hipCm;
    private Double armCm;
    private Double thighCm;
    private LocalDate recordedDate;
    private String notes;
}