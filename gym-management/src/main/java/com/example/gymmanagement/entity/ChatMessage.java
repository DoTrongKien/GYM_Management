package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String sender; // USER hoặc BOT

    @Column(length = 2000)
    private String content;

    // ── Đính kèm (ảnh / video / file bất kỳ) ──
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentType;
    private Long   attachmentSize;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
