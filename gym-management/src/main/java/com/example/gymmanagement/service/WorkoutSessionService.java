package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.CheckInRequest;
import com.example.gymmanagement.dto.request.EnrollSessionRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.SessionStatus;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutSessionService {

    private final WorkoutSessionRepository sessionRepository;
    private final SessionExerciseLogRepository logRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanDayRepository planDayRepository;
    private final NotificationService notificationService;

    // ── Đăng ký 1 buổi tập từ planDay ────────────────────────
    @Transactional
    public WorkoutSessionResponse enrollSession(String email, EnrollSessionRequest req) {
        User user = getUser(email);

        WorkoutPlanDay planDay = null;
        WorkoutPlan plan = null;

        if (req.getPlanDayId() != null) {
            planDay = planDayRepository.findById(req.getPlanDayId())
                    .orElseThrow(() -> new RuntimeException("Ngày tập không tồn tại"));
            plan = planDay.getWorkoutPlan();

            // Kiểm tra đã đăng ký chưa
            if (req.getWeekNumber() != null &&
                    sessionRepository.existsByUserIdAndPlanDayIdAndWeekNumber(
                            user.getId(), req.getPlanDayId(), req.getWeekNumber())) {
                throw new RuntimeException("Bạn đã đăng ký ngày tập này cho tuần " + req.getWeekNumber() + " rồi!");
            }
        } else if (req.getPlanId() != null) {
            plan = planRepository.findById(req.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Giáo án không tồn tại"));
        }

        WorkoutSession session = WorkoutSession.builder()
                .user(user)
                .workoutPlan(plan)
                .planDay(planDay)
                .sessionDate(req.getSessionDate())
                .scheduledTime(req.getScheduledTime())
                .weekNumber(req.getWeekNumber())
                .customSessionName(req.getCustomSessionName())
                .isCustom(req.getPlanDayId() == null)
                .status(SessionStatus.SCHEDULED)
                .build();

        sessionRepository.save(session);

        // Thông báo
        String name = planDay != null ? planDay.getDayName()
                : (req.getCustomSessionName() != null ? req.getCustomSessionName() : "Buổi tập");
        String timeStr = req.getScheduledTime() != null ? " lúc " + req.getScheduledTime() : "";
        notificationService.sendToUser(user.getId(),
                "📅 Đã đăng ký lịch tập",
                "\"" + name + "\" vào " + req.getSessionDate() + timeStr,
                "WORKOUT_REMINDER");

        return buildResponse(session);
    }

    // ── Xem sessions của tôi ──────────────────────────────────
    public List<WorkoutSessionResponse> getMySessions(String email) {
        return sessionRepository.findByUserIdOrderBySessionDateDesc(getUser(email).getId())
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public List<WorkoutSessionResponse> getWeekSessions(String email) {
        User user = getUser(email);
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        return sessionRepository.findByUserIdAndSessionDateBetweenOrderBySessionDate(
                        user.getId(), monday, monday.plusDays(6))
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public WorkoutSessionResponse getSessionById(String email, Long id) {
        WorkoutSession s = getOwned(email, id);
        return buildResponse(s);
    }

    // ── Tiến trình đăng ký theo tuần ─────────────────────────
    public Map<String, Object> getWeekProgress(String email, Long planId, Integer weekNumber) {
        User user = getUser(email);
        long enrolled  = sessionRepository.countEnrolledInWeek(user.getId(), planId, weekNumber);
        long completed = sessionRepository.countCompletedInWeek(user.getId(), planId, weekNumber);

        // Lấy plan để biết sessionsPerWeek
        var plan = planRepository.findById(planId).orElseThrow();
        int target = plan.getSessionsPerWeek();

        Map<String, Object> result = new HashMap<>();
        result.put("weekNumber",    weekNumber);
        result.put("enrolled",      enrolled);
        result.put("completed",     completed);
        result.put("target",        target);
        result.put("isWeekDone",    completed >= target);
        result.put("canGoNextWeek", completed >= target);
        return result;
    }

    // ── Check-in ─────────────────────────────────────────────
    @Transactional
    public WorkoutSessionResponse checkIn(String email, Long id) {
        WorkoutSession s = getOwned(email, id);
        if (s.getStatus() == SessionStatus.CHECKED_IN)
            throw new RuntimeException("Đã check-in rồi!");
        if (s.getStatus() == SessionStatus.COMPLETED)
            throw new RuntimeException("Buổi tập đã hoàn thành!");
        s.setStatus(SessionStatus.CHECKED_IN);
        s.setCheckInTime(LocalDateTime.now());
        sessionRepository.save(s);
        return buildResponse(s);
    }

    // ── Hoàn thành ───────────────────────────────────────────
    @Transactional
    public WorkoutSessionResponse completeSession(String email, Long id, CheckInRequest req) {
        User user = getUser(email);
        WorkoutSession s = getOwned(email, id);

        if (req.getExerciseLogs() != null && !req.getExerciseLogs().isEmpty()) {
            List<SessionExerciseLog> logs = req.getExerciseLogs().stream().map(r -> {
                Exercise ex = exerciseRepository.findById(r.getExerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise not found"));
                return SessionExerciseLog.builder()
                        .session(s).exercise(ex)
                        .setsCompleted(r.getSetsCompleted()).repsCompleted(r.getRepsCompleted())
                        .durationSeconds(r.getDurationSeconds()).weightUsedKg(r.getWeightUsedKg())
                        .isCompleted(r.getIsCompleted() != null ? r.getIsCompleted() : true)
                        .notes(r.getNotes()).build();
            }).collect(Collectors.toList());
            logRepository.saveAll(logs);

            int cal = logs.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsCompleted()) && l.getExercise().getCaloriesBurned() != null)
                    .mapToInt(l -> l.getExercise().getCaloriesBurned() * (l.getSetsCompleted() != null ? l.getSetsCompleted() : 1))
                    .sum();
            s.setTotalCaloriesBurned(cal);
        }

        s.setStatus(SessionStatus.COMPLETED);
        s.setCheckOutTime(LocalDateTime.now());
        if (s.getCheckInTime() != null) {
            long mins = java.time.Duration.between(s.getCheckInTime(), s.getCheckOutTime()).toMinutes();
            s.setDurationMinutes((int) mins);
        }
        sessionRepository.save(s);

        // Kiểm tra xem tuần này đã hoàn thành chưa → thông báo
        if (s.getWorkoutPlan() != null && s.getWeekNumber() != null) {
            long completed = sessionRepository.countCompletedInWeek(
                    user.getId(), s.getWorkoutPlan().getId(), s.getWeekNumber());
            int target = s.getWorkoutPlan().getSessionsPerWeek();
            if (completed >= target) {
                notificationService.sendToUser(user.getId(),
                        "🎉 Hoàn thành tuần " + s.getWeekNumber() + "!",
                        "Bạn đã hoàn thành tất cả " + target + " buổi tập tuần này! Hãy đăng ký tuần " + (s.getWeekNumber() + 1) + ".",
                        "SYSTEM");
            }
        }

        return buildResponse(s);
    }

    @Transactional
    public WorkoutSessionResponse skipSession(String email, Long id, String notes) {
        WorkoutSession s = getOwned(email, id);
        s.setStatus(SessionStatus.SKIPPED);
        s.setNotes(notes);
        sessionRepository.save(s);
        return buildResponse(s);
    }

    @Transactional
    public void deleteSession(String email, Long id) {
        WorkoutSession s = getOwned(email, id);
        if (s.getStatus() == SessionStatus.COMPLETED)
            throw new RuntimeException("Không thể xóa buổi tập đã hoàn thành");
        sessionRepository.delete(s);
    }

    // ── Build response ────────────────────────────────────────
    public WorkoutSessionResponse buildResponse(WorkoutSession s) {
        List<SessionExerciseLog> logs = logRepository.findBySessionId(s.getId());
        List<ExerciseLogResponse> logResp = logs.stream().map(l ->
                ExerciseLogResponse.builder()
                        .id(l.getId()).exerciseId(l.getExercise().getId())
                        .exerciseName(l.getExercise().getName())
                        .setsCompleted(l.getSetsCompleted()).repsCompleted(l.getRepsCompleted())
                        .durationSeconds(l.getDurationSeconds()).weightUsedKg(l.getWeightUsedKg())
                        .isCompleted(l.getIsCompleted()).notes(l.getNotes()).build()
        ).collect(Collectors.toList());

        // Lấy bài tập mẫu từ planDay để hiển thị khi check-in
        List<WorkoutPlanExerciseResponse> planExercises = Collections.emptyList();
        if (s.getPlanDay() != null && s.getPlanDay().getExercises() != null) {
            planExercises = s.getPlanDay().getExercises().stream().map(pe ->
                    WorkoutPlanExerciseResponse.builder()
                            .id(pe.getId()).exerciseId(pe.getExercise().getId())
                            .exerciseName(pe.getExercise().getName())
                            .muscleGroup(pe.getExercise().getMuscleGroup() != null ? pe.getExercise().getMuscleGroup().name() : null)
                            .difficulty(pe.getExercise().getDifficulty() != null ? pe.getExercise().getDifficulty().name() : null)
                            .sets(pe.getSets()).reps(pe.getReps()).durationSeconds(pe.getDurationSeconds())
                            .restSeconds(pe.getRestSeconds()).orderIndex(pe.getOrderIndex())
                            .notes(pe.getNotes()).videoUrl(pe.getExercise().getVideoUrl())
                            .caloriesBurned(pe.getExercise().getCaloriesBurned()).build()
            ).collect(Collectors.toList());
        }

        return WorkoutSessionResponse.builder()
                .id(s.getId()).sessionDate(s.getSessionDate()).scheduledTime(s.getScheduledTime())
                .checkInTime(s.getCheckInTime()).checkOutTime(s.getCheckOutTime())
                .status(s.getStatus()).totalCaloriesBurned(s.getTotalCaloriesBurned())
                .durationMinutes(s.getDurationMinutes()).notes(s.getNotes())
                .weekNumber(s.getWeekNumber())
                .planName(s.getWorkoutPlan() != null ? s.getWorkoutPlan().getPlanName() : null)
                .dayName(s.getPlanDay() != null ? s.getPlanDay().getDayName() : null)
                .customSessionName(s.getCustomSessionName()).isCustom(s.getIsCustom())
                .exerciseLogs(logResp).planExercises(planExercises)
                .build();
    }

    private WorkoutSession getOwned(String email, Long id) {
        User user = getUser(email);
        WorkoutSession s = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!s.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        return s;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}