package com.example.gymmanagement.entity;

import com.example.gymmanagement.enums.SupportStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Một phiên hỗ trợ chat 1:1 giữa user và admin. */
@Entity
@Table(name = "support_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;   // người gửi yêu cầu

    private String subject;   // tiêu đề / vấn đề cần hỗ trợ (mỗi cuộc hội thoại một vấn đề riêng)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;  // admin xử lý (null khi chưa có ai nhận)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SupportStatus status = SupportStatus.PENDING;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime acceptedAt;
    private LocalDateTime closedAt;
}
