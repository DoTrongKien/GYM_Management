package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.SupportSession;
import com.example.gymmanagement.enums.SupportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportSessionRepository extends JpaRepository<SupportSession, Long> {

    // Phiên đang mở (PENDING/ACTIVE) mới nhất của user
    Optional<SupportSession> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
            Long userId, List<SupportStatus> statuses);

    // Tất cả phiên đang mở của user (cho phép nhiều cuộc hội thoại cùng lúc)
    List<SupportSession> findByUserIdAndStatusInOrderByCreatedAtDesc(
            Long userId, List<SupportStatus> statuses);

    // Phiên gần nhất của user (bất kể trạng thái)
    Optional<SupportSession> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    // Danh sách phiên theo trạng thái (cho admin)
    List<SupportSession> findByStatusInOrderByCreatedAtDesc(List<SupportStatus> statuses);
}
