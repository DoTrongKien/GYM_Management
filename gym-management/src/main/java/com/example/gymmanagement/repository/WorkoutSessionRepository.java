package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.WorkoutSession;
import com.example.gymmanagement.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    List<WorkoutSession> findByUserIdOrderBySessionDateDesc(Long userId);

    List<WorkoutSession> findByUserIdAndSessionDateBetweenOrderBySessionDate(
            Long userId, LocalDate start, LocalDate end);

    List<WorkoutSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    boolean existsByUserIdAndPlanDayIdAndWeekNumber(Long userId, Long planDayId, Integer weekNumber);

    @Query("SELECT COUNT(s) FROM WorkoutSession s WHERE s.user.id=:uid AND s.workoutPlan.id=:planId AND s.weekNumber=:week")
    long countEnrolledInWeek(@Param("uid") Long uid, @Param("planId") Long planId, @Param("week") Integer week);

    @Query("SELECT COUNT(s) FROM WorkoutSession s WHERE s.user.id=:uid AND s.workoutPlan.id=:planId AND s.weekNumber=:week AND s.status='COMPLETED'")
    long countCompletedInWeek(@Param("uid") Long uid, @Param("planId") Long planId, @Param("week") Integer week);

    @Query("SELECT AVG(s.completionRate) FROM WorkoutSession s WHERE s.user.id=:uid AND s.workoutPlan.id=:planId AND s.weekNumber=:week AND s.status='COMPLETED' AND s.completionRate IS NOT NULL")
    Double avgCompletionRateInWeek(@Param("uid") Long uid, @Param("planId") Long planId, @Param("week") Integer week);

    @Query("SELECT s FROM WorkoutSession s WHERE s.user.id=:uid AND s.workoutPlan.id=:planId AND s.weekNumber=:week ORDER BY s.sessionDate")
    List<WorkoutSession> findByPlanAndWeek(@Param("uid") Long uid, @Param("planId") Long planId, @Param("week") Integer week);

    @Query("SELECT s FROM WorkoutSession s WHERE s.user.id=:uid AND s.workoutPlan.id=:planId AND s.sessionDate IS NOT NULL ORDER BY s.sessionDate ASC, s.id ASC")
    List<WorkoutSession> findByPlanOrderBySessionDate(@Param("uid") Long uid, @Param("planId") Long planId);


    @Query("SELECT s FROM WorkoutSession s WHERE s.user.id=:uid AND s.workoutPlan.id=:planId AND s.weekNumber=:week AND s.isLastSessionOfWeek=true")
    List<WorkoutSession> findLastSessionOfWeek(@Param("uid") Long uid, @Param("planId") Long planId, @Param("week") Integer week);

    @Query("SELECT COUNT(s) FROM WorkoutSession s WHERE s.user.id=:uid AND s.status='COMPLETED'")
    Long countCompletedByUserId(@Param("uid") Long uid);

    @Query("SELECT SUM(s.totalCaloriesBurned) FROM WorkoutSession s WHERE s.user.id=:uid AND s.status='COMPLETED'")
    Long sumCaloriesByUserId(@Param("uid") Long uid);

    @Query("SELECT s FROM WorkoutSession s WHERE s.sessionDate=:date AND s.status='SCHEDULED'")
    List<WorkoutSession> findScheduledSessionsForDate(@Param("date") LocalDate date);

    @Query("SELECT s FROM WorkoutSession s WHERE s.sessionDate=:date AND s.scheduledTime BETWEEN :from AND :to AND s.status='SCHEDULED'")
    List<WorkoutSession> findAllUpcomingSessions(@Param("date") LocalDate date,
                                                 @Param("from") LocalTime from,
                                                 @Param("to") LocalTime to);

    // Lấy tất cả session của user theo plan (dùng để xóa khi tạo plan mới)
    @Query("SELECT s FROM WorkoutSession s WHERE s.user.id = :userId AND s.workoutPlan.id = :planId")
    List<WorkoutSession> findByUserIdAndWorkoutPlanId(
            @Param("userId") Long userId,
            @Param("planId") Long planId);

    // ==================== THÊM MỚI ĐỂ XÓA AN TOÀN KHI ĐIỀU CHỈNH GIÁO ÁN ====================
    void deleteByPlanDayId(Long planDayId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WorkoutSession s WHERE s.planDay.id IN :planDayIds")
    void deleteByPlanDayIds(@Param("planDayIds") List<Long> planDayIds);

    List<WorkoutSession> findByPlanDayId(Long planDayId);
}