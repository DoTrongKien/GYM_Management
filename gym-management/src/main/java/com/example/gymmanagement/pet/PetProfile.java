// ============================================================
// GHI ĐÈ FILE: src/main/java/com/example/gymmanagement/pet/PetProfile.java
// (bỏ hẳn field petScore cũ - stage giờ chỉ phụ thuộc BMI, không còn
//  ý nghĩa tổng hợp điểm nữa)
// ============================================================
package com.example.gymmanagement.pet;

import com.example.gymmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;
import java.time.LocalDateTime;

@Entity
@Table(name = "pet_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PetProfile implements Persistable<Long> {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // Thể hình - CHỈ phụ thuộc chỉ số cơ thể (BMI), không còn bị ảnh hưởng bởi streak/adherence
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PetStage stage = PetStage.AVERAGE;

    // Chuỗi buổi tập COMPLETED liên tiếp gần nhất
    @Builder.Default
    private Integer currentStreak = 0;

    // Chuỗi buổi bị bỏ liên tiếp gần nhất (SKIPPED hoặc SCHEDULED trôi qua ngày không check-in)
    @Builder.Default
    private Integer missedStreak = 0;

    // Bậc hào quang hiển thị quanh pet, suy ra từ currentStreak
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuraTier auraTier = AuraTier.NONE;

    // Số mạng nhện hiển thị quanh pet, suy ra từ missedStreak (0-6)
    @Builder.Default
    private Integer webCount = 0;

    private LocalDateTime lastCalculatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public Long getId() { return userId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }
}