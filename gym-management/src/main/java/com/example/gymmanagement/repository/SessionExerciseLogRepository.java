package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.SessionExerciseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionExerciseLogRepository extends JpaRepository<SessionExerciseLog, Long> {

    List<SessionExerciseLog> findBySessionId(Long sessionId);

    // ── MỚI: lấy toàn bộ log trong 1 tuần của user + plan, dùng để tính điều chỉnh tạ theo nhóm cơ ──
    @Query("""
        SELECT l FROM SessionExerciseLog l
        JOIN l.session s
        WHERE s.user.id = :userId AND s.workoutPlan.id = :planId AND s.weekNumber = :weekNumber
    """)
    List<SessionExerciseLog> findByUserIdAndPlanIdAndWeekNumber(
            @Param("userId") Long userId,
            @Param("planId") Long planId,
            @Param("weekNumber") Integer weekNumber);
}