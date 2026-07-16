package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "endurance_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnduranceTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private Integer pushupReps;
    private Integer plankSeconds;
    private Integer squatReps;

    private LocalDateTime testedAt;
}