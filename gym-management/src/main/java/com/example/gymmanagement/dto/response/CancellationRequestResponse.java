package com.example.gymmanagement.dto.response;

import com.example.gymmanagement.enums.CancellationStatus;
import com.example.gymmanagement.enums.MembershipType;
import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CancellationRequestResponse {
    private Long id;
    private Long membershipId;
    private MembershipType membershipType;
    private Long userId;
    private String userName;
    private String userEmail;
    private String reason;
    private CancellationStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String processedByName;
    private String adminNote;
    private Double refundAmount;
    private Boolean refunded;
}