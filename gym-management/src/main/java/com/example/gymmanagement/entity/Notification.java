package com.example.gymmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String title;
    private String message;
    private String type; // EMAIL_REMINDER, WORKOUT_REMINDER, PROMOTION, SYSTEM

    // Đối tượng mà thông báo trỏ tới, để bấm vào là nhảy thẳng tới nó.
    // Lưu loại + id thay vì đường dẫn, vì cùng một đánh giá thì user và admin
    // mở ở hai trang khác nhau — frontend tự quyết định dựa trên vai trò.
    private String refType;   // RATING, ...
    private Long   refId;
    // @Builder.Default: không có nó, builder sẽ bỏ qua giá trị mặc định và gán null
    // → isRead=null nên không bị countByUserIdAndIsReadFalse đếm, chấm đỏ không bao giờ hiện.
    @Builder.Default
    private Boolean isRead = false;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}