package com.example.gymmanagement.dto.response;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageResponse {
    private Long id;
    private String sender; // USER hoặc BOT
    private String content;
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentType;
    private Long   attachmentSize;
    private LocalDateTime createdAt;
}
