package com.example.gymmanagement.dto.response;
import com.example.gymmanagement.enums.MembershipType;
import com.example.gymmanagement.enums.PaymentStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private MembershipType membershipType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double price;
    private Boolean isActive;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private Integer daysRemaining;
}