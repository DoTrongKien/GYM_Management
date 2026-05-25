package com.example.gymmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private User user;

    private Integer rating;
    private String comment;
    private String serviceType;

    @Builder.Default
    private Boolean isPublic = true;

    private String adminReply;
    private LocalDateTime repliedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}