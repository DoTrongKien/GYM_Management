package com.example.gymmanagement.dto.response;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatResponse {
    private String reply;              // Câu trả lời của bot
    private List<String> suggestions;  // Các tin nhắn gợi ý tiếp theo
    private boolean understood;        // Bot có hiểu câu hỏi hay không
    private LocalDateTime createdAt;
}
