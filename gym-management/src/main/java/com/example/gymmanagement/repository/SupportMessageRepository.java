package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
