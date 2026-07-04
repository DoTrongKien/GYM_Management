package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.CancellationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "membership_cancellation_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipCancellationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CancellationStatus status = CancellationStatus.PENDING_REVIEW;

    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(length = 1000)
    private String adminNote;

    private Double refundAmount;
    private Boolean refunded;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
        if (refunded == null) refunded = false;
    }
}