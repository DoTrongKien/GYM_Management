package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Tin nhắn trong một phiên hỗ trợ. */
@Entity
@Table(name = "support_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SupportSession session;

    private String senderRole; // USER hoặc ADMIN

    @Column(length = 2000)
    private String content;

    // ── Đính kèm (ảnh / video / file bất kỳ) ──
    private String attachmentUrl;   // đường dẫn tải: /api/files/support/<uuid>.<ext>
    private String attachmentName;  // tên file gốc
    private String attachmentType;  // MIME type
    private Long   attachmentSize;  // dung lượng (bytes)

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
