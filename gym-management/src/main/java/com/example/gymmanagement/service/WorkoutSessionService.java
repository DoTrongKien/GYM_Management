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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import com.example.gymmanagement.service.Workoutplanhelper;

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
    private final Workoutplanhelper workoutPlanHelper;
    private WorkoutPlanService workoutPlanService;
    @org.springframework.beans.factory.annotation.Autowired
    public void setWorkoutPlanService(@Lazy WorkoutPlanService workoutPlanService) {
        this.workoutPlanService = workoutPlanService;
    }

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
// ── Check-out ─────────────────────────────────────────────
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
            throw new RuntimeException("Đây là buổi cuối tuần! Vui lòng nhập cân nặng hiện tại.");

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
                    .mapToInt(l -> l.getExercise().getCaloriesBurned()
                            * (l.getSetsCompleted() != null ? l.getSetsCompleted() : 1))
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

        boolean isTemplatePlan = s.getWorkoutPlan() != null
                && Boolean.FALSE.equals(s.getWorkoutPlan().getIsAiGenerated());
        boolean isAiPlan = s.getWorkoutPlan() != null
                && Boolean.TRUE.equals(s.getWorkoutPlan().getIsAiGenerated());

        if (Boolean.TRUE.equals(s.getIsLastSessionOfWeek()) && req.getCheckoutWeight() != null) {

            // Lưu tiến độ cơ thể
            progressService.autoSaveProgress(
                    user,
                    req.getCheckoutWeight(),
                    req.getCheckoutBodyFat(),
                    "Tự động ghi nhận sau tuần " + s.getWeekNumber(),
                    ProgressSource.WEEKLY_CHECKOUT,
                    s.getSessionDate()
            );

            profileRepo.findByUserId(user.getId()).ifPresent(profile -> {
                profile.setWeight(req.getCheckoutWeight());
                if (req.getCheckoutBodyFat() != null)
                    profile.setBodyFatPercentage(req.getCheckoutBodyFat());
                profileRepo.save(profile);
            });

            // === Gợi ý tăng/giảm tạ — áp dụng cho CẢ 2 loại plan ===
            applyWeightAdjustmentNote(user, s.getWorkoutPlan(), s.getWeekNumber());

            if (isTemplatePlan) {
                // Plan template: tự tăng tuần, không cần action riêng
                advanceTemplatePlanWeek(s.getWorkoutPlan());

            } else if (isAiPlan) {
                // Plan AI: gộp adjustPlanAfterWeek() vào đây luôn
                // Dễ chỉnh sau: chỉ cần sửa WorkoutPlanService.adjustPlanAfterWeek()
                // mà không cần đụng vào controller hay FE
                try {
                    workoutPlanService.adjustPlanAfterWeek(
                            s.getWorkoutPlan().getId(),
                            email,
                            req.getCheckoutWeight(),
                            req.getCheckoutBodyFat()
                    );
                } catch (Exception e) {
                    // Log lỗi nhưng không fail check-out
                    // Người dùng vẫn hoàn thành buổi tập bình thường
                    notifService.sendToUser(user.getId(),
                            "⚠️ Lưu ý",
                            "Buổi tập đã hoàn thành nhưng căn chỉnh tuần mới gặp sự cố. Vui lòng thử lại.",
                            "SYSTEM");
                }
            }
        }

        // Thông báo kết quả
        String msg = req.getCompletionRate() >= 90
                ? "🔥 Xuất sắc! " + req.getCompletionRate() + "% hoàn thành!"
                : req.getCompletionRate() >= 70
                  ? "✅ Tốt! " + req.getCompletionRate() + "% hoàn thành."
                  : "💪 " + req.getCompletionRate() + "% — cố gắng hơn buổi sau nhé!";
        notifService.sendToUser(user.getId(), "Kết quả buổi tập", msg, "SYSTEM");

        if (Boolean.TRUE.equals(s.getIsLastSessionOfWeek())) {
            String nextMsg = isTemplatePlan
                    ? "Dữ liệu đã ghi nhận. Giáo án tự động chuyển sang tuần tiếp theo."
                    : "Dữ liệu đã ghi nhận và giáo án đã được căn chỉnh cho tuần mới.";
            notifService.sendToUser(user.getId(),
                    "📊 Hoàn thành tuần " + s.getWeekNumber() + "!",
                    nextMsg, "SYSTEM");
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
                .planId(s.getWorkoutPlan() != null ? s.getWorkoutPlan().getId() : null)  // === MỚI
                .planName(s.getWorkoutPlan()!=null ? s.getWorkoutPlan().getPlanName() : null)
                .dayName(s.getPlanDay()!=null       ? s.getPlanDay().getDayName()    : null)
                .customSessionName(s.getCustomSessionName()).isCustom(s.getIsCustom())
                .completionRate(s.getCompletionRate())
                .isLastSessionOfWeek(s.getIsLastSessionOfWeek())
                .checkoutWeight(s.getCheckoutWeight()).checkoutBodyFat(s.getCheckoutBodyFat())
                .exerciseLogs(logs).planExercises(planExs)
                // === MỚI ===
                .dayMismatchWarning(buildDayMismatchWarning(s))
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


    // ── buildDayMismatchWarning — hỗ trợ CẢ 2 loại plan ─────────
    private String buildDayMismatchWarning(WorkoutSession s) {
        WorkoutPlan plan = s.getWorkoutPlan();
        if (plan == null || s.getSessionDate() == null) return null;

        boolean isAiPlan = Boolean.TRUE.equals(plan.getIsAiGenerated());
        boolean isTemplatePlan = Boolean.FALSE.equals(plan.getIsAiGenerated());

        List<Integer> allowedDows;

        if (isTemplatePlan) {
            // Plan template: lấy từ planDays.dayOfWeek
            List<WorkoutPlanDay> planDays = plan.getPlanDays() != null
                    ? plan.getPlanDays()
                    : dayRepo.findByWorkoutPlanIdOrderByDayOfWeek(plan.getId());
            if (planDays == null || planDays.isEmpty()) return null;

            allowedDows = planDays.stream()
                    .map(WorkoutPlanDay::getDayOfWeek)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

        } else if (isAiPlan) {
            // Plan AI: lấy từ suggestedDays (tên tiếng Anh) → map sang ISO dow
            List<String> suggested = workoutPlanHelper.suggestDays(plan.getGoal(), plan.getSessionsPerWeek());
            if (suggested == null || suggested.isEmpty()) return null;

            allowedDows = suggested.stream()
                    .map(workoutPlanHelper::dayNameToIsoDow)
                    .filter(d -> d > 0)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            return null;
        }

        if (allowedDows.isEmpty()) return null;

        // Lấy toàn bộ session theo plan, sort theo sessionDate
        List<WorkoutSession> allSessions = sessionRepo.findByPlanOrderBySessionDate(
                s.getUser().getId(), plan.getId());
        if (allSessions.isEmpty()) return null;

        // Buổi 1 của toàn giáo án — xác định điểm neo chu kỳ
        WorkoutSession firstSession = allSessions.get(0);
        int firstDow = firstSession.getSessionDate().getDayOfWeek().getValue();

        List<Integer> rotatedCycle;
        int idxInAllowed = allowedDows.indexOf(firstDow);
        if (idxInAllowed >= 0) {
            // Buổi 1 khớp allowedDows → xoay chu kỳ từ vị trí đó
            rotatedCycle = new ArrayList<>();
            int size = allowedDows.size();
            for (int i = 0; i < size; i++) {
                rotatedCycle.add(allowedDows.get((idxInAllowed + i) % size));
            }
        } else {
            // Buổi 1 sai → dùng allowedDows gốc làm chuẩn (không xoay)
            rotatedCycle = allowedDows;
        }

        // Xác định buổi đang xét là thứ mấy trong toàn giáo án
        int indexOfCurrent = -1;
        for (int i = 0; i < allSessions.size(); i++) {
            if (allSessions.get(i).getId().equals(s.getId())) {
                indexOfCurrent = i;
                break;
            }
        }
        if (indexOfCurrent < 0) return null;

        int expectedDow = rotatedCycle.get(indexOfCurrent % rotatedCycle.size());
        int actualDow = s.getSessionDate().getDayOfWeek().getValue();

        if (actualDow != expectedDow) {
            String expectedName = dowVietnameseName(expectedDow);
            String actualName = dowVietnameseName(actualDow);
            return "⚠️ Theo chu kỳ tập của bạn, buổi này nên rơi vào " + expectedName
                    + " nhưng bạn đang tập vào " + actualName
                    + ". Tập không đúng chu kỳ có thể làm giáo án không đạt hiệu quả tối ưu.";
        }
        return null;
    }

    private String dowVietnameseName(int dow) {
        return switch (dow) {
            case 1 -> "Thứ Hai";
            case 2 -> "Thứ Ba";
            case 3 -> "Thứ Tư";
            case 4 -> "Thứ Năm";
            case 5 -> "Thứ Sáu";
            case 6 -> "Thứ Bảy";
            case 7 -> "Chủ Nhật";
            default -> "?";
        };
    }

    // ─────────────────────────────────────────────────────────
    // Helper MỚI: tính & lưu weightAdjustmentNote cho plan từ template
    // dựa trên % hoàn thành TRUNG BÌNH của tuần vừa hoàn thành.
    // CHỈ gọi khi plan.isAiGenerated == false.
    // ─────────────────────────────────────────────────────────
    // ── applyWeightAdjustmentNote — áp dụng cho CẢ 2 loại plan ──
    // (xóa guard isAiGenerated=false cũ, giờ gọi cho tất cả)

    private void applyWeightAdjustmentNote(User user, WorkoutPlan plan, Integer weekNumber) {
        if (plan == null) return;
        Double avgRate = sessionRepo.avgCompletionRateInWeek(user.getId(), plan.getId(), weekNumber);
        if (avgRate == null) return;

        String note;
        if (avgRate >= 90) {
            note = "💪 Tuần này bạn hoàn thành " + Math.round(avgRate) + "%! Nên tăng tạ khoảng 10% so với tuần trước.";
        } else if (avgRate >= 80) {
            note = "📈 Tuần này bạn hoàn thành " + Math.round(avgRate) + "%. Nên tăng tạ khoảng 5% so với tuần trước.";
        } else if (avgRate >= 70) {
            note = "✅ Tuần này bạn hoàn thành " + Math.round(avgRate) + "%. Giữ nguyên mức tạ hiện tại.";
        } else if (avgRate >= 60) {
            note = "📉 Tuần này bạn hoàn thành " + Math.round(avgRate) + "%. Nên giảm tạ khoảng 5% để đảm bảo form đúng.";
        } else {
            note = "⚠️ Tuần này bạn hoàn thành " + Math.round(avgRate) + "%. Nên giảm tạ khoảng 10% và tập trung vào kỹ thuật.";
        }

        plan.setWeightAdjustmentNote(note);
        planRepo.save(plan);
    }

    private void advanceTemplatePlanWeek(WorkoutPlan plan) {
        int nextWeek = (plan.getCurrentWeek() != null ? plan.getCurrentWeek() : 1) + 1;
        plan.setCurrentWeek(nextWeek);
        if (nextWeek > plan.getDurationWeeks()) {
            plan.setIsCompleted(true);
            plan.setIsActive(false);
        }
        planRepo.save(plan);
    }

}