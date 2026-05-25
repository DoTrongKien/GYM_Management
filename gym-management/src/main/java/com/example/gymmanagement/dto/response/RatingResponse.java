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
    private String comment;
    private String serviceType;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    // admin reply
    private String adminReply;
    private LocalDateTime repliedAt;
}