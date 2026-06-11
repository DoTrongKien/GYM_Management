package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.ProgressSource;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress_tracking")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProgressTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Double weight;
    private Double height;
    private Double bmi;
    private Double bodyFatPercentage;
    private Double muscleMassKg;

    // Body measurements in cm
    private Double chestCm;
    private Double waistCm;
    private Double hipCm;
    private Double armCm;
    private Double thighCm;

    private LocalDate recordedDate;
    private LocalDateTime recordedAt = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    private ProgressSource source;
    private String notes;
}