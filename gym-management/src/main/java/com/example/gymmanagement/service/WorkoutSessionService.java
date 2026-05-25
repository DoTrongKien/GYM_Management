package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.CheckInRequest;
import com.example.gymmanagement.dto.request.ScheduleSessionRequest;
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
    private final WorkoutPlanDayRepository planDayRepository;
    private final NotificationService notificationService;

    public List<WorkoutSessionResponse> getMySessions(String email) {
        User user = getUser(email);
        return sessionRepository.findByUserIdOrderBySessionDateDesc(user.getId())
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public List<WorkoutSessionResponse> getWeekSessions(String email) {
        User user = getUser(email);
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        return sessionRepository.findByUserIdAndSessionDateBetweenOrderBySessionDate(user.getId(), monday, sunday)
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public WorkoutSessionResponse getSessionById(String email, Long sessionId) {
        User user = getUser(email);
        WorkoutSession session = getSessionOwned(user, sessionId);
        return buildResponse(session);
    }

    // ── Custom schedule ──────────────────────────────────────
    @Transactional
    public WorkoutSessionResponse scheduleCustomSession(String email, ScheduleSessionRequest request) {
        User user = getUser(email);

        WorkoutPlanDay planDay = null;
        if (request.getPlanDayId() != null) {
            planDay = planDayRepository.findById(request.getPlanDayId()).orElse(null);
        }

        WorkoutSession session = WorkoutSession.builder()
                .user(user)
                .sessionDate(request.getSessionDate())
                .scheduledTime(request.getScheduledTime())
                .customSessionName(request.getCustomSessionName())
                .planDay(planDay)
                .isCustom(true)
                .status(SessionStatus.SCHEDULED)
                .build();
        sessionRepository.save(session);

        // Push notification
        String timeStr = request.getScheduledTime() != null ? " lúc " + request.getScheduledTime() : "";
        notificationService.sendToUser(user.getId(),
                "📅 Lịch tập đã đặt",
                "Buổi tập \"" + (request.getCustomSessionName() != null ? request.getCustomSessionName() : "Tập luyện")
                        + "\" vào ngày " + request.getSessionDate() + timeStr + " đã được lên lịch!",
                "WORKOUT_REMINDER");

        return buildResponse(session);
    }

    // ── Check-in ─────────────────────────────────────────────
    @Transactional
    public WorkoutSessionResponse checkIn(String email, Long sessionId) {
        User user = getUser(email);
        WorkoutSession session = getSessionOwned(user, sessionId);

        if (session.getStatus() == SessionStatus.CHECKED_IN)
            throw new RuntimeException("Đã check-in rồi!");
        if (session.getStatus() == SessionStatus.COMPLETED)
            throw new RuntimeException("Buổi tập đã hoàn thành!");

        session.setStatus(SessionStatus.CHECKED_IN);
        session.setCheckInTime(LocalDateTime.now());
        sessionRepository.save(session);
        return buildResponse(session);
    }

    // ── Complete ─────────────────────────────────────────────
    @Transactional
    public WorkoutSessionResponse completeSession(String email, Long sessionId, CheckInRequest request) {
        User user = getUser(email);
        WorkoutSession session = getSessionOwned(user, sessionId);

        if (request.getExerciseLogs() != null && !request.getExerciseLogs().isEmpty()) {
            List<SessionExerciseLog> logs = request.getExerciseLogs().stream().map(req -> {
                Exercise exercise = exerciseRepository.findById(req.getExerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise not found: " + req.getExerciseId()));
                return SessionExerciseLog.builder()
                        .session(session).exercise(exercise)
                        .setsCompleted(req.getSetsCompleted()).repsCompleted(req.getRepsCompleted())
                        .durationSeconds(req.getDurationSeconds()).weightUsedKg(req.getWeightUsedKg())
                        .isCompleted(req.getIsCompleted() != null ? req.getIsCompleted() : true)
                        .notes(req.getNotes()).build();
            }).collect(Collectors.toList());
            logRepository.saveAll(logs);

            int totalCal = logs.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsCompleted()) && l.getExercise().getCaloriesBurned() != null)
                    .mapToInt(l -> l.getExercise().getCaloriesBurned() * (l.getSetsCompleted() != null ? l.getSetsCompleted() : 1))
                    .sum();
            session.setTotalCaloriesBurned(totalCal);
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setCheckOutTime(LocalDateTime.now());
        if (session.getCheckInTime() != null) {
            long mins = java.time.Duration.between(session.getCheckInTime(), session.getCheckOutTime()).toMinutes();
            session.setDurationMinutes((int) mins);
        }
        sessionRepository.save(session);
        return buildResponse(session);
    }

    @Transactional
    public WorkoutSessionResponse skipSession(String email, Long sessionId, String notes) {
        User user = getUser(email);
        WorkoutSession session = getSessionOwned(user, sessionId);
        session.setStatus(SessionStatus.SKIPPED);
        session.setNotes(notes);
        sessionRepository.save(session);
        return buildResponse(session);
    }

    @Transactional
    public void deleteSession(String email, Long sessionId) {
        User user = getUser(email);
        WorkoutSession session = getSessionOwned(user, sessionId);
        if (session.getStatus() == SessionStatus.COMPLETED)
            throw new RuntimeException("Không thể xóa buổi tập đã hoàn thành");
        sessionRepository.delete(session);
    }

    // ── Helpers ───────────────────────────────────────────────
    private WorkoutSession getSessionOwned(User user, Long sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Access denied");
        return session;
    }

    public WorkoutSessionResponse buildResponse(WorkoutSession s) {
        List<SessionExerciseLog> logs = logRepository.findBySessionId(s.getId());
        List<ExerciseLogResponse> logResponses = logs.stream().map(log ->
                ExerciseLogResponse.builder()
                        .id(log.getId())
                        .exerciseId(log.getExercise().getId())
                        .exerciseName(log.getExercise().getName())
                        .setsCompleted(log.getSetsCompleted()).repsCompleted(log.getRepsCompleted())
                        .durationSeconds(log.getDurationSeconds()).weightUsedKg(log.getWeightUsedKg())
                        .isCompleted(log.getIsCompleted()).notes(log.getNotes()).build()
        ).collect(Collectors.toList());

        return WorkoutSessionResponse.builder()
                .id(s.getId())
                .sessionDate(s.getSessionDate())
                .scheduledTime(s.getScheduledTime())
                .checkInTime(s.getCheckInTime())
                .checkOutTime(s.getCheckOutTime())
                .status(s.getStatus())
                .totalCaloriesBurned(s.getTotalCaloriesBurned())
                .durationMinutes(s.getDurationMinutes())
                .notes(s.getNotes())
                .weekNumber(s.getWeekNumber())
                .planName(s.getWorkoutPlan() != null ? s.getWorkoutPlan().getPlanName() : null)
                .dayName(s.getPlanDay() != null ? s.getPlanDay().getDayName() : null)
                .customSessionName(s.getCustomSessionName())
                .isCustom(s.getIsCustom())
                .exerciseLogs(logResponses)
                .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}