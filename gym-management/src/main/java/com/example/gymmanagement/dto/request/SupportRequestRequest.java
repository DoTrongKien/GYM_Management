package com.example.gymmanagement.dto.request;
import lombok.*;

/** Yêu cầu tạo một cuộc hội thoại hỗ trợ mới với admin. */
@Data @NoArgsConstructor @AllArgsConstructor
public class SupportRequestRequest {
    private String subject;   // vấn đề cần hỗ trợ
}
