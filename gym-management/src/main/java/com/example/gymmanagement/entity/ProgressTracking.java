package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "progress_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Double weight;

    private Double bmi;

    private LocalDateTime recordedAt = LocalDateTime.now();
}
