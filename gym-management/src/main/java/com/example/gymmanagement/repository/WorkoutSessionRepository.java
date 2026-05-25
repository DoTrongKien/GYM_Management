package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.WorkoutSession;
import com.example.gymmanagement.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    List<WorkoutSession> findByUserIdOrderBySessionDateDesc(Long userId);
    List<WorkoutSession> findByUserIdAndSessionDateBetweenOrderBySessionDate(Long userId, LocalDate start, LocalDate end);
    List<WorkoutSession> findByUserIdAndStatus(Long userId, SessionStatus status);
    List<WorkoutSession> findByWorkoutPlanIdOrderBySessionDateAsc(Long planId);

    @Query("SELECT COUNT(s) FROM WorkoutSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'")
    Long countCompletedByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(s.totalCaloriesBurned) FROM WorkoutSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'")
    Long sumCaloriesByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM WorkoutSession s WHERE s.sessionDate = :date AND s.status = 'SCHEDULED'")
    List<WorkoutSession> findScheduledSessionsForDate(@Param("date") LocalDate date);

    // Remind: sessions scheduled today within next N minutes
    @Query("SELECT s FROM WorkoutSession s WHERE s.user.id = :userId AND s.sessionDate = :date AND s.scheduledTime IS NOT NULL AND s.scheduledTime BETWEEN :from AND :to AND s.status = 'SCHEDULED'")
    List<WorkoutSession> findUpcomingSessions(@Param("userId") Long userId,
                                              @Param("date") LocalDate date,
                                              @Param("from") LocalTime from,
                                              @Param("to") LocalTime to);

    @Query("SELECT s FROM WorkoutSession s WHERE s.sessionDate = :date AND s.scheduledTime IS NOT NULL AND s.scheduledTime BETWEEN :from AND :to AND s.status = 'SCHEDULED'")
    List<WorkoutSession> findAllUpcomingSessions(@Param("date") LocalDate date,
                                                 @Param("from") LocalTime from,
                                                 @Param("to") LocalTime to);
}