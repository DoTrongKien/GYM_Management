package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
// daanh gia cua admin
@Entity
@Table(name = "service_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Integer rating;
    private String title;        // tiêu đề đánh giá (tùy chọn)
    private String comment;
    private String serviceType;  // loại dịch vụ (tùy chọn, có thể null)

    @Builder.Default
    private Boolean isPublic = true;

    // File user đính kèm vào đánh giá (tùy chọn)
    private String attachmentUrl;   // đường dẫn tải: /api/files/ratings/<uuid>.<ext>
    private String attachmentName;  // tên file gốc
    private String attachmentType;  // MIME type
    private Long   attachmentSize;  // dung lượng (bytes)

    private String adminReply;
    private LocalDateTime repliedAt;

    // File admin đính kèm trong phản hồi (tùy chọn)
    private String replyAttachmentUrl;   // đường dẫn tải: /api/files/ratings/<uuid>.<ext>
    private String replyAttachmentName;  // tên file gốc
    private String replyAttachmentType;  // MIME type
    private Long   replyAttachmentSize;  // dung lượng (bytes)

    // ✅ FIX CHẮC CHẮN
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;     // thời điểm user sửa đánh giá gần nhất

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

}