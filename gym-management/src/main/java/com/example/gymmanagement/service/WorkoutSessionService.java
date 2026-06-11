package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.*;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.ProgressSource;
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

    private final WorkoutSessionRepository  sessionRepo;
    private final SessionExerciseLogRepository logRepo;
    private final ExerciseRepository        exerciseRepo;
    private final UserRepository            userRepo;
    private final WorkoutPlanRepository     planRepo;
    private final WorkoutPlanDayRepository  dayRepo;
    private final NotificationService       notifService;
    private final UserProfileRepository     profileRepo;
    private final ProgressService progressService;

    // ── Đăng ký buổi tập ─────────────────────────────────────
    @Transactional
    public WorkoutSessionResponse enrollSession(String email, EnrollSessionRequest req) {
        User user = getUser(email);

        WorkoutPlanDay planDay = null;
        WorkoutPlan plan = null;

        if (req.getPlanDayId() != null) {
            planDay = dayRepo.findById(req.getPlanDayId())
                    .orElseThrow(() -> new RuntimeException("Ngày tập không tồn tại"));
            plan = planDay.getWorkoutPlan();

            if (req.getWeekNumber() != null &&
                    sessionRepo.existsByUserIdAndPlanDayIdAndWeekNumber(
                            user.getId(), req.getPlanDayId(), req.getWeekNumber()))
                throw new RuntimeException("Bạn đã đăng ký ngày tập này trong tuần " + req.getWeekNumber());

            // Kiểm tra số buổi đã đăng ký trong tuần chưa vượt max
            if (req.getWeekNumber() != null && plan != null) {
                long enrolled = sessionRepo.countEnrolledInWeek(user.getId(), plan.getId(), req.getWeekNumber());
                if (enrolled >= plan.getSessionsPerWeek())
                    throw new RuntimeException("Đã đủ " + plan.getSessionsPerWeek() + " buổi cho tuần này!");
            }
        } else if (req.getPlanId() != null) {
            plan = planRepo.findById(req.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Giáo án không tồn tại"));
        }

        // Cập nhật weekStartDate nếu đây là buổi đầu tiên của tuần 1
        if (plan != null && plan.getWeekStartDate() == null && req.getWeekNumber() == 1) {
            plan.setWeekStartDate(req.getSessionDate());
            planRepo.save(plan);
        }

        boolean isLast = Boolean.TRUE.equals(req.getIsLastSessionOfWeek());
        // Tự xác định buổi cuối nếu không truyền
        if (!isLast && plan != null && req.getWeekNumber() != null) {
            long current = sessionRepo.countEnrolledInWeek(user.getId(), plan.getId(), req.getWeekNumber());
            isLast = (current + 1) >= plan.getSessionsPerWeek();
        }

        WorkoutSession session = WorkoutSession.builder()
                .user(user).workoutPlan(plan).planDay(planDay)
                .sessionDate(req.getSessionDate()).scheduledTime(req.getScheduledTime())
                .weekNumber(req.getWeekNumber())
                .isLastSessionOfWeek(isLast)
                .customSessionName(req.getCustomSessionName())
                .isCustom(req.getPlanDayId() == null)
                .status(SessionStatus.SCHEDULED)
                .build();
        sessionRepo.save(session);

        String name    = planDay != null ? planDay.getDayName()
                : (req.getCustomSessionName() != null ? req.getCustomSessionName() : "Buổi tập");
        String timeStr = req.getScheduledTime() != null ? " lúc " + req.getScheduledTime() : "";
        notifService.sendToUser(user.getId(), "📅 Đã đăng ký lịch tập",
                "\"" + name + "\" vào " + req.getSessionDate() + timeStr, "WORKOUT_REMINDER");

        return buildResponse(session);
    }

    // ── Tiến trình tuần ──────────────────────────────────────
    public Map<String, Object> getWeekProgress(String email, Long planId, Integer weekNumber) {
        User user = getUser(email);
        WorkoutPlan plan = planRepo.findById(planId).orElseThrow();
        long enrolled  = sessionRepo.countEnrolledInWeek(user.getId(), planId, weekNumber);
        long completed = sessionRepo.countCompletedInWeek(user.getId(), planId, weekNumber);
        int  target    = plan.getSessionsPerWeek();
        Double avgRate = sessionRepo.avgCompletionRateInWeek(user.getId(), planId, weekNumber);

        // Kiểm tra buổi cuối đã checkout chưa
        boolean lastCheckedOut = sessionRepo.findLastSessionOfWeek(user.getId(), planId, weekNumber)
                .stream().anyMatch(s -> s.getStatus() == SessionStatus.COMPLETED && s.getCheckoutWeight() != null);

        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("weekNumber",       weekNumber);
        r.put("enrolled",         enrolled);
        r.put("completed",        completed);
        r.put("target",           target);
        r.put("isWeekDone",       completed >= target);
        r.put("canGoNextWeek",    completed >= target && lastCheckedOut);
        r.put("avgCompletionRate",avgRate);
        r.put("currentPlanWeek",  plan.getCurrentWeek());
        r.put("totalWeeks",       plan.getDurationWeeks());
        r.put("setsAdj",          plan.getSetsAdjustment());
        r.put("repsAdj",          plan.getRepsAdjustment());
        return r;
    }

    // ── Xem sessions ─────────────────────────────────────────
    public List<WorkoutSessionResponse> getMySessions(String email) {
        return sessionRepo.findByUserIdOrderBySessionDateDesc(getUser(email).getId())
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public List<WorkoutSessionResponse> getWeekSessions(String email) {
        User u = getUser(email);
        LocalDate mon = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        return sessionRepo.findByUserIdAndSessionDateBetweenOrderBySessionDate(u.getId(), mon, mon.plusDays(6))
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public WorkoutSessionResponse getSessionById(String email, Long id) {
        return buildResponse(getOwned(email, id));
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
        sessionRepo.save(s);
        return buildResponse(s);
    }

    // ── Check-out (bắt buộc nhập tỉ lệ hoàn thành) ──────────
    @Transactional
    public WorkoutSessionResponse checkOut(String email, Long id, CheckOutRequest req) {
        User user = getUser(email);
        WorkoutSession s = getOwned(email, id);

        if (s.getStatus() != SessionStatus.CHECKED_IN)
            throw new RuntimeException("Hãy check-in trước khi check-out!");
        if (req.getCompletionRate() == null)
            throw new RuntimeException("Vui lòng nhập tỉ lệ hoàn thành (0-100%)!");
        if (req.getCompletionRate() < 0 || req.getCompletionRate() > 100)
            throw new RuntimeException("Tỉ lệ hoàn thành phải từ 0 đến 100!");

        // Buổi cuối tuần bắt buộc nhập cân nặng
        if (Boolean.TRUE.equals(s.getIsLastSessionOfWeek()) && req.getCheckoutWeight() == null)
            throw new RuntimeException("Đây là buổi cuối tuần! Vui lòng nhập cân nặng hiện tại để hệ thống điều chỉnh giáo án.");

        // Lưu exercise logs
        if (req.getExerciseLogs() != null && !req.getExerciseLogs().isEmpty()) {
            List<SessionExerciseLog> logs = req.getExerciseLogs().stream().map(r -> {
                Exercise ex = exerciseRepo.findById(r.getExerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise not found"));
                return SessionExerciseLog.builder()
                        .session(s).exercise(ex)
                        .setsCompleted(r.getSetsCompleted()).repsCompleted(r.getRepsCompleted())
                        .durationSeconds(r.getDurationSeconds()).weightUsedKg(r.getWeightUsedKg())
                        .isCompleted(r.getIsCompleted() != null ? r.getIsCompleted() : true)
                        .notes(r.getNotes()).build();
            }).collect(Collectors.toList());
            logRepo.saveAll(logs);

            int cal = logs.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsCompleted()) && l.getExercise().getCaloriesBurned() != null)
                    .mapToInt(l -> l.getExercise().getCaloriesBurned() * (l.getSetsCompleted() != null ? l.getSetsCompleted() : 1))
                    .sum();
            s.setTotalCaloriesBurned(cal);
        }

        s.setStatus(SessionStatus.COMPLETED);
        s.setCheckOutTime(LocalDateTime.now());
        s.setCompletionRate(req.getCompletionRate());
        s.setNotes(req.getNotes());
        if (req.getCheckoutWeight()  != null) s.setCheckoutWeight(req.getCheckoutWeight());
        if (req.getCheckoutBodyFat() != null) s.setCheckoutBodyFat(req.getCheckoutBodyFat());

        if (s.getCheckInTime() != null) {
            long mins = java.time.Duration.between(s.getCheckInTime(), s.getCheckOutTime()).toMinutes();
            s.setDurationMinutes((int) mins);
        }
        sessionRepo.save(s);

        if (Boolean.TRUE.equals(s.getIsLastSessionOfWeek())
                && req.getCheckoutWeight() != null) {

            progressService.autoSaveProgress(
                    user,
                    req.getCheckoutWeight(),
                    req.getCheckoutBodyFat(),
                    "Tự động ghi nhận sau tuần "
                            + s.getWeekNumber(),
                    ProgressSource.WEEKLY_CHECKOUT,
                    s.getSessionDate() //mới thêm
            );

            profileRepo.findByUserId(user.getId())
                    .ifPresent(profile -> {

                        profile.setWeight(
                                req.getCheckoutWeight()
                        );

                        if (req.getCheckoutBodyFat() != null) {
                            profile.setBodyFatPercentage(
                                    req.getCheckoutBodyFat()
                            );
                        }

                        profileRepo.save(profile);
                    });
        }

        // Thông báo kết quả
        String msg = req.getCompletionRate() >= 90 ? "🔥 Xuất sắc! " + req.getCompletionRate() + "% hoàn thành!"
                : req.getCompletionRate() >= 70 ? "✅ Tốt! " + req.getCompletionRate() + "% hoàn thành."
                  : "💪 " + req.getCompletionRate() + "% — cố gắng hơn buổi sau nhé!";
        notifService.sendToUser(user.getId(), "Kết quả buổi tập", msg, "SYSTEM");

        // Nếu là buổi cuối tuần → thông báo cần điều chỉnh giáo án
        if (Boolean.TRUE.equals(s.getIsLastSessionOfWeek())) {
            notifService.sendToUser(user.getId(),
                    "📊 Hoàn thành tuần " + s.getWeekNumber() + "!",
                    "Dữ liệu đã ghi nhận. Hãy vào trang Giáo án để xem điều chỉnh cho tuần tiếp theo.",
                    "SYSTEM");
        }

        return buildResponse(s);
    }

    @Transactional
    public WorkoutSessionResponse skipSession(String email, Long id, String notes) {
        WorkoutSession s = getOwned(email, id);
        s.setStatus(SessionStatus.SKIPPED); s.setNotes(notes);
        sessionRepo.save(s);
        return buildResponse(s);
    }

    @Transactional
    public void deleteSession(String email, Long id) {
        WorkoutSession s = getOwned(email, id);
        if (s.getStatus() == SessionStatus.COMPLETED)
            throw new RuntimeException("Không thể xóa buổi đã hoàn thành");
        sessionRepo.delete(s);
    }

    // ── Build response ────────────────────────────────────────
    public WorkoutSessionResponse buildResponse(WorkoutSession s) {
        List<ExerciseLogResponse> logs = logRepo.findBySessionId(s.getId()).stream().map(l ->
                ExerciseLogResponse.builder()
                        .id(l.getId()).exerciseId(l.getExercise().getId())
                        .exerciseName(l.getExercise().getName())
                        .setsCompleted(l.getSetsCompleted()).repsCompleted(l.getRepsCompleted())
                        .durationSeconds(l.getDurationSeconds()).weightUsedKg(l.getWeightUsedKg())
                        .isCompleted(l.getIsCompleted()).notes(l.getNotes()).build()
        ).collect(Collectors.toList());

        List<WorkoutPlanExerciseResponse> planExs = Collections.emptyList();
        if (s.getPlanDay() != null && s.getPlanDay().getExercises() != null) {
            planExs = s.getPlanDay().getExercises().stream().map(pe ->
                    WorkoutPlanExerciseResponse.builder()
                            .id(pe.getId()).exerciseId(pe.getExercise().getId())
                            .exerciseName(pe.getExercise().getName())
                            .muscleGroup(pe.getExercise().getMuscleGroup()!=null ? pe.getExercise().getMuscleGroup().name() : null)
                            .difficulty(pe.getExercise().getDifficulty()!=null   ? pe.getExercise().getDifficulty().name()  : null)
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
                .planName(s.getWorkoutPlan()!=null ? s.getWorkoutPlan().getPlanName() : null)
                .dayName(s.getPlanDay()!=null       ? s.getPlanDay().getDayName()    : null)
                .customSessionName(s.getCustomSessionName()).isCustom(s.getIsCustom())
                .completionRate(s.getCompletionRate())
                .isLastSessionOfWeek(s.getIsLastSessionOfWeek())
                .checkoutWeight(s.getCheckoutWeight()).checkoutBodyFat(s.getCheckoutBodyFat())
                .exerciseLogs(logs).planExercises(planExs)
                .build();
    }

    private WorkoutSession getOwned(String email, Long id) {
        User u = getUser(email);
        WorkoutSession s = sessionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!s.getUser().getId().equals(u.getId())) throw new RuntimeException("Access denied");
        return s;
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}