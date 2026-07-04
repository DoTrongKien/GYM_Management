package com.example.gymmanagement.dto.response;

import com.example.gymmanagement.enums.MembershipType;
import com.example.gymmanagement.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private MembershipType membershipType;
    private Double price;
    private Long membershipId;
    private PaymentStatus status;
    private String transferCode;
    private String qrRawPayload;
    private String payUrl;
    private String qrCodeUrl;
    private String deeplink;
    private String transactionId;
    private String resultMessage;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private Long secondsRemaining; // đếm ngược cho FE, 0 nếu đã hết hạn/không còn PENDING
}