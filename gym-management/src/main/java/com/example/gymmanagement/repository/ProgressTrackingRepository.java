package com.example.gymmanagement.repository;
import com.example.gymmanagement.entity.ProgressTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface ProgressTrackingRepository extends JpaRepository<ProgressTracking, Long> {
    List<ProgressTracking> findByUserIdOrderByRecordedDateDesc(Long userId);
    Optional<ProgressTracking> findFirstByUserIdOrderByRecordedDateDesc(Long userId);
    @Query("SELECT p FROM ProgressTracking p WHERE p.user.id = :userId ORDER BY p.recordedDate ASC")
    List<ProgressTracking> findByUserIdOrderByDateAsc(@Param("userId") Long userId);
}