package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.MembershipType;
import com.example.gymmanagement.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "memberships")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private MembershipType membershipType;

    private LocalDate startDate;
    private LocalDate endDate;
    private Double price;
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String transactionId;
    private String paymentMethod; // CARD, MOMO, VNPAY, ZALOPAY, CASH
    private LocalDateTime paidAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String notes;
}