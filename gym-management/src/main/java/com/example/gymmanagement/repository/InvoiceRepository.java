package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.Invoice;
import com.example.gymmanagement.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Invoice> findByMomoOrderId(String momoOrderId);
    Optional<Invoice> findByTransferCode(String transferCode);
    List<Invoice> findByStatusAndExpiresAtBefore(PaymentStatus status, LocalDateTime time);
    List<Invoice> findByStatus(PaymentStatus status);
    Optional<Invoice> findByUserIdAndStatus(Long userId, PaymentStatus status);
    List<Invoice> findAllByOrderByCreatedAtDesc();
}