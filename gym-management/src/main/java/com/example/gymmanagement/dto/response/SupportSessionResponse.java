package com.example.gymmanagement.dto.response;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportSessionResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String subject;       // tiêu đề / vấn đề của cuộc hội thoại
    private String adminName;
    private String status;        // PENDING / ACTIVE / REJECTED / CLOSED
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private String lastMessage;   // nội dung tin nhắn cuối (cho danh sách của admin)
}
