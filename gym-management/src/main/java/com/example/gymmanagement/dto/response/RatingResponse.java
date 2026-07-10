package com.example.gymmanagement.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Integer rating;
    private String title;
    private String comment;
    private String serviceType;   // null nếu user không chọn dịch vụ
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;   // null nếu user chưa sửa lần nào
    // file user đính kèm vào đánh giá
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentType;
    private Long   attachmentSize;
    // admin reply
    private String adminReply;
    private LocalDateTime repliedAt;
    // file admin đính kèm trong phản hồi
    private String replyAttachmentUrl;
    private String replyAttachmentName;
    private String replyAttachmentType;
    private Long   replyAttachmentSize;
}