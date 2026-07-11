package com.example.gymmanagement.dto.request;
import lombok.*;

/** Yêu cầu tạo một cuộc hội thoại hỗ trợ mới với admin. */
@Data @NoArgsConstructor @AllArgsConstructor
public class SupportRequestRequest {
    private String subject;   // tiêu đề vấn đề cần hỗ trợ
    private String content;   // nội dung user muốn trình bày (tin nhắn mở đầu)
}
