package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.MembershipType;
import com.example.gymmanagement.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private MembershipType membershipType;

    private Double price;

    // Gói tập được kích hoạt khi hóa đơn này PAID (null cho tới khi thanh toán thành công)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // ── MoMo (không dùng nữa - giữ lại phòng khi cần bật lại) ────
    @Column(unique = true)
    private String momoOrderId;      // orderId gửi cho MoMo, đổi mỗi lần tạo/tạo lại QR
    private String momoRequestId;
    private String payUrl;           // link thanh toán MoMo (mobile deeplink fallback)
    private String qrCodeUrl;        // link ảnh QR (VietQR API - phương án dự phòng)
    private String deeplink;
    private String transactionId;    // mã tham chiếu giao dịch ngân hàng khi khớp lệnh
    private String resultMessage;    // nội dung/chi tiết giao dịch (để debug/đối soát)

    // ── Chuyển khoản ngân hàng (VietQR + SePay webhook) ──────────
    @Column(unique = true)
    private String transferCode;     // mã duy nhất nhúng trong nội dung chuyển khoản, vd "GYMPRO6"
    @Column(length = 1000)
    private String qrRawPayload;     // chuỗi payload VietQR chuẩn EMVCo, FE tự render QR từ chuỗi này

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt; // createdAt + 5 phút
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;

    private Integer regenerateCount; // số lần user bấm "tạo lại QR"

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (regenerateCount == null) regenerateCount = 0;
    }
}