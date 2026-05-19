package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.CheckInRequest;
import com.example.gymmanagement.dto.request.ExerciseLogRequest;
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
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return buildResponse(session);
    }

    @Transactional
    public WorkoutSessionResponse checkIn(String email, Long sessionId) {
        User user = getUser(email);
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        if (session.getStatus() == SessionStatus.CHECKED_IN) {
            throw new RuntimeException("Already checked in for this session");
        }
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new RuntimeException("Session already completed");
        }

        session.setStatus(SessionStatus.CHECKED_IN);
        session.setCheckInTime(LocalDateTime.now());
        sessionRepository.save(session);
        return buildResponse(session);
    }

    @Transactional
    public WorkoutSessionResponse completeSession(String email, Long sessionId, CheckInRequest request) {
        User user = getUser(email);
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Save exercise logs
        if (request.getExerciseLogs() != null) {
            List<SessionExerciseLog> logs = request.getExerciseLogs().stream().map(req -> {
                Exercise exercise = exerciseRepository.findById(req.getExerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise not found: " + req.getExerciseId()));
                return SessionExerciseLog.builder()
                        .session(session)
                        .exercise(exercise)
                        .setsCompleted(req.getSetsCompleted())
                        .repsCompleted(req.getRepsCompleted())
                        .durationSeconds(req.getDurationSeconds())
                        .weightUsedKg(req.getWeightUsedKg())
                        .isCompleted(req.getIsCompleted() != null ? req.getIsCompleted() : true)
                        .notes(req.getNotes())
                        .build();
            }).collect(Collectors.toList());
            logRepository.saveAll(logs);

            // Calculate total calories
            int totalCalories = logs.stream()
                    .filter(log -> log.getIsCompleted() && log.getExercise().getCaloriesBurned() != null)
                    .mapToInt(log -> {
                        int cal = log.getExercise().getCaloriesBurned();
                        int sets = log.getSetsCompleted() != null ? log.getSetsCompleted() : 1;
                        return cal * sets;
                    }).sum();
            session.setTotalCaloriesBurned(totalCalories);
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setCheckOutTime(LocalDateTime.now());

        if (session.getCheckInTime() != null) {
            long minutes = java.time.Duration.between(session.getCheckInTime(), session.getCheckOutTime()).toMinutes();
            session.setDurationMinutes((int) minutes);
        }

        sessionRepository.save(session);

        // Analyze performance and adjust if needed (week 4+)
        analyzeAndAdjust(user.getId(), session);

        return buildResponse(session);
    }

    @Transactional
    public WorkoutSessionResponse skipSession(String email, Long sessionId, String notes) {
        User user = getUser(email);
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");

        session.setStatus(SessionStatus.SKIPPED);
        session.setNotes(notes);
        sessionRepository.save(session);
        return buildResponse(session);
    }

    private void analyzeAndAdjust(Long userId, WorkoutSession completedSession) {
        if (completedSession.getWeekNumber() != null && completedSession.getWeekNumber() >= 4) {
            // Analyze last 2 weeks completion rate
            List<WorkoutSession> planSessions = sessionRepository.findByWorkoutPlanIdOrderBySessionDateAsc(
                    completedSession.getWorkoutPlan().getId());

            long completed = planSessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
            long total = planSessions.stream().filter(s -> s.getSessionDate().isBefore(LocalDate.now())).count();

            if (total > 0) {
                double rate = (double) completed / total;
                // Could flag for plan adjustment based on performance
                // rate > 0.8 → increase intensity, rate < 0.5 → decrease intensity
            }
        }
    }

    private WorkoutSessionResponse buildResponse(WorkoutSession s) {
        List<SessionExerciseLog> logs = logRepository.findBySessionId(s.getId());
        List<ExerciseLogResponse> logResponses = logs.stream().map(log ->
                ExerciseLogResponse.builder()
                        .id(log.getId())
                        .exerciseId(log.getExercise().getId())
                        .exerciseName(log.getExercise().getName())
                        .setsCompleted(log.getSetsCompleted())
                        .repsCompleted(log.getRepsCompleted())
                        .durationSeconds(log.getDurationSeconds())
                        .weightUsedKg(log.getWeightUsedKg())
                        .isCompleted(log.getIsCompleted())
                        .notes(log.getNotes())
                        .build()
        ).collect(Collectors.toList());

        return WorkoutSessionResponse.builder()
                .id(s.getId())
                .sessionDate(s.getSessionDate())
                .checkInTime(s.getCheckInTime())
                .checkOutTime(s.getCheckOutTime())
                .status(s.getStatus())
                .totalCaloriesBurned(s.getTotalCaloriesBurned())
                .durationMinutes(s.getDurationMinutes())
                .notes(s.getNotes())
                .weekNumber(s.getWeekNumber())
                .planName(s.getWorkoutPlan() != null ? s.getWorkoutPlan().getPlanName() : null)
                .dayName(s.getPlanDay() != null ? s.getPlanDay().getDayName() : null)
                .exerciseLogs(logResponses)
                .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}