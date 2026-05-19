package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_ratings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer rating; // 1-5
    private String comment;
    private String serviceType; // WORKOUT_PLAN, NUTRITION, FACILITY, TRAINER
    private LocalDateTime createdAt = LocalDateTime.now();
    private Boolean isPublic = true;
}