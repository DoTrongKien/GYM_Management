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

    List<WorkoutSession> findByUserIdAndSessionDateBetweenOrderBySessionDate(
            Long userId, LocalDate start, LocalDate end);

    List<WorkoutSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    List<WorkoutSession> findByUserIdAndWorkoutPlanIdOrderBySessionDateAsc(Long userId, Long planId);

    // Kiểm tra user đã đăng ký planDay này trong tuần chưa
    boolean existsByUserIdAndPlanDayIdAndWeekNumber(Long userId, Long planDayId, Integer weekNumber);

    // Đếm số buổi đã đăng ký trong 1 tuần của plan
    @Query("SELECT COUNT(s) FROM WorkoutSession s WHERE s.user.id = :uid AND s.workoutPlan.id = :planId AND s.weekNumber = :week")
    long countEnrolledInWeek(@Param("uid") Long uid, @Param("planId") Long planId, @Param("week") Integer week);

    // Đếm số buổi đã COMPLETED trong 1 tuần
    @Query("SELECT COUNT(s) FROM WorkoutSession s WHERE s.user.id = :uid AND s.workoutPlan.id = :planId AND s.weekNumber = :week AND s.status = 'COMPLETED'")
    long countCompletedInWeek(@Param("uid") Long uid, @Param("planId") Long planId, @Param("week") Integer week);

    // Lịch sử completed cho dashboard
    @Query("SELECT COUNT(s) FROM WorkoutSession s WHERE s.user.id = :uid AND s.status = 'COMPLETED'")
    Long countCompletedByUserId(@Param("uid") Long uid);

    @Query("SELECT SUM(s.totalCaloriesBurned) FROM WorkoutSession s WHERE s.user.id = :uid AND s.status = 'COMPLETED'")
    Long sumCaloriesByUserId(@Param("uid") Long uid);

    // Sessions theo ngày (cho scheduler nhắc nhở)
    @Query("SELECT s FROM WorkoutSession s WHERE s.sessionDate = :date AND s.status = 'SCHEDULED'")
    List<WorkoutSession> findScheduledSessionsForDate(@Param("date") LocalDate date);

    @Query("SELECT s FROM WorkoutSession s WHERE s.sessionDate = :date AND s.scheduledTime BETWEEN :from AND :to AND s.status = 'SCHEDULED'")
    List<WorkoutSession> findAllUpcomingSessions(@Param("date") LocalDate date,
                                                 @Param("from") LocalTime from,
                                                 @Param("to") LocalTime to);
}