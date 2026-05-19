package com.example.gymmanagement.repository;
import com.example.gymmanagement.entity.Membership;
import com.example.gymmanagement.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Membership> findByUserIdAndIsActiveTrue(Long userId);
    List<Membership> findAllByOrderByCreatedAtDesc();
    List<Membership> findByPaymentStatus(PaymentStatus status);
    @Query("SELECT SUM(m.price) FROM Membership m WHERE m.paymentStatus = 'PAID' AND m.paidAt BETWEEN :start AND :end")
    Double sumRevenueBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
    @Query("SELECT m FROM Membership m WHERE m.endDate = :date AND m.isActive = true")
    List<Membership> findExpiringOnDate(@Param("date") LocalDate date);
}