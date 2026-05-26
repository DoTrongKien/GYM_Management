package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
// daanh gia cua admin
@Entity
@Table(name = "service_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Integer rating;
    private String comment;
    private String serviceType;

    @Builder.Default
    private Boolean isPublic = true;

    private String adminReply;
    private LocalDateTime repliedAt;

    // ✅ FIX CHẮC CHẮN
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

}