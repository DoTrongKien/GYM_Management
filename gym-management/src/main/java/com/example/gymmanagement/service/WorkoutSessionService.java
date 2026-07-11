package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.*;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.MuscleGroup;
import com.example.gymmanagement.enums.ProgressSource;
import com.example.gymmanagement.enums.SessionStatus;
import com.example.gymmanagement.pet.PetService;
import com.example.gymmanagement.repository.*;
import com.example.gymmanagement.service.schedule.ScheduleCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;

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
    private WorkoutPlanService workoutPlanService;
    @org.springframework.beans.factory.annotation.Autowired
    public void setWorkoutPlanService(@Lazy WorkoutPlanService workoutPlanService) {
        this.workoutPlanService = workoutPlanService;
    }
    private PetService petService;
    @org.springframework.beans.factory.annotation.Autowired
    public void setPetService(@Lazy PetService petService) {
        this.petService = petService;
    }

    // ── mana + điều chỉnh tạ theo nhóm cơ ──
    private final ManaService manaService;
    private final WorkoutPlanMuscleGroupWeightRepository mgWeightRepo;
    private final WorkoutPlanExerciseRepository planExerciseRepo;

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

            if (req.getWeekNumber() != null && plan != null) {
                long enrolled = sessionRepo.countEnrolledInWeek(user.getId(), plan.getId(), req.getWeekNumber());
                if (enrolled >= plan.getSessionsPerWeek())
                    throw new RuntimeException("Đã đủ " + plan.getSessionsPerWeek() + " buổi cho tuần này!");
            }
        } else if (req.getPlanId() != null) {
            plan = planRepo.findById(req.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Giáo án không tồn tại"));
        }

        if (plan != null && plan.getWeekStartDate() == null && req.getWeekNumber() == 1) {
            plan.setWeekStartDate(req.getSessionDate());
            planRepo.save(plan);
        }

        boolean isLast = Boolean.TRUE.equals(req.getIsLastSessionOfWeek());
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
    // ── Mana giờ chỉ đại diện cho khả năng HỒI PHỤC, KHÔNG còn ảnh hưởng Set/Rep ──
    // - Nếu mana không đủ cho buổi (estimatedCost > currentMana) và chưa xác nhận:
    //     trả về requiresConfirmation=true, KHÔNG check-in thật, để FE hiện popup.
    // - Nếu người dùng xác nhận "vẫn tập" (confirmReducedIntensity=true), hoặc nếu mana
    //     đủ: check-in bình thường. Set/Rep hiển thị LUÔN giữ nguyên theo giáo án gốc.
    @Transactional
    public CheckInResult checkIn(String email, Long id, boolean confirmReducedIntensity) {
        WorkoutSession s = getOwned(email, id);
        if (s.getStatus() == SessionStatus.CHECKED_IN)
            throw new RuntimeException("Đã check-in rồi!");
        if (s.getStatus() == SessionStatus.COMPLETED)
            throw new RuntimeException("Buổi tập đã hoàn thành!");

        WorkoutPlan plan = s.getWorkoutPlan();
        boolean hasManaSystem = plan != null && plan.getMaxMana() != null;

        if (hasManaSystem && s.getPlanDay() != null && s.getPlanDay().getExercises() != null
                && !s.getPlanDay().getExercises().isEmpty()) {

            int estimatedCost = s.getPlanDay().getExercises().stream()
                    .mapToInt(pe -> pe.getExercise().getStaminaCost() != null
                            ? pe.getExercise().getStaminaCost() : 10)
                    .sum();
            int currentMana = manaService.getCurrentManaAfterRegen(plan);

            if (currentMana < estimatedCost && !confirmReducedIntensity) {
                return CheckInResult.builder()
                        .requiresConfirmation(true)
                        .warningMessage("⚠️ Thể lực hiện tại không đủ để hồi phục tối ưu sau buổi tập. " +
                                "Nếu vẫn tiếp tục tập, cơ thể có thể hồi phục chậm hơn và tăng nguy cơ quá tải.")
                        .estimatedManaCost(estimatedCost)
                        .currentMana(currentMana)
                        .session(null)
                        .build();
            }
            // Dù mana đủ hay đã xác nhận "vẫn tập": check-in bình thường, KHÔNG chỉnh
            // sửa Set/Rep hay bất kỳ dữ liệu hiển thị nào của buổi tập.
        }

        s.setStatus(SessionStatus.CHECKED_IN);
        s.setCheckInTime(LocalDateTime.now());
        sessionRepo.save(s);

        return CheckInResult.builder()
                .requiresConfirmation(false)
                .session(buildResponse(s))
                .build();
    }

    // ── Check-out ─────────────────────────────────────────────
    @Transactional
    public WorkoutSessionResponse checkOut(String email, Long id, CheckOutRequest req) {
        User user = getUser(email);
        WorkoutSession s = getOwned(email, id);

        if (s.getStatus() != SessionStatus.CHECKED_IN)
            throw new RuntimeException("Hãy check-in trước khi check-out!");
        if (req.getExerciseLogs() == null || req.getExerciseLogs().isEmpty())
            throw new RuntimeException("Vui lòng chọn tỉ lệ hoàn thành cho từng bài tập!");

        // Buổi cuối tuần bắt buộc nhập cân nặng
        if (Boolean.TRUE.equals(s.getIsLastSessionOfWeek()) && req.getCheckoutWeight() == null)
            throw new RuntimeException("Đây là buổi cuối tuần! Vui lòng nhập cân nặng hiện tại.");

        // ── Lưu exercise logs theo completionPercent (0/25/50/75/100) ──
        List<SessionExerciseLog> logs = req.getExerciseLogs().stream().map(r -> {
            Exercise ex = exerciseRepo.findById(r.getExerciseId())
                    .orElseThrow(() -> new RuntimeException("Exercise not found"));
            if (r.getCompletionPercent() == null)
                throw new RuntimeException("Vui lòng chọn tỉ lệ hoàn thành cho bài: " + ex.getName());
            return SessionExerciseLog.builder()
                    .session(s).exercise(ex)
                    .completionPercent(r.getCompletionPercent())
                    .weightUsedKg(r.getWeightUsedKg())
                    .isCompleted(r.getCompletionPercent() > 0)
                    .notes(r.getNotes())
                    .build();
        }).collect(Collectors.toList());
        logRepo.saveAll(logs);

        // completionRate tổng của buổi = trung bình cộng completionPercent các bài
        double avgSessionRate = logs.stream()
                .filter(l -> l.getCompletionPercent() != null)
                .mapToInt(SessionExerciseLog::getCompletionPercent)
                .average().orElse(0);
        int sessionCompletionRate = (int) Math.round(avgSessionRate);
        s.setCompletionRate(sessionCompletionRate);

        int cal = logs.stream()
                .filter(l -> l.getCompletionPercent() != null && l.getCompletionPercent() > 0
                        && l.getExercise().getCaloriesBurned() != null)
                .mapToInt(l -> Math.round(l.getExercise().getCaloriesBurned() * (l.getCompletionPercent() / 100f)))
                .sum();
        s.setTotalCaloriesBurned(cal);

        s.setStatus(SessionStatus.COMPLETED);
        s.setCheckOutTime(LocalDateTime.now());
        s.setNotes(req.getNotes());
        if (req.getCheckoutWeight()  != null) s.setCheckoutWeight(req.getCheckoutWeight());
        if (req.getCheckoutBodyFat() != null) s.setCheckoutBodyFat(req.getCheckoutBodyFat());

        if (s.getCheckInTime() != null) {
            long mins = java.time.Duration.between(s.getCheckInTime(), s.getCheckOutTime()).toMinutes();
            s.setDurationMinutes((int) mins);
        }
        sessionRepo.save(s);

        // ── Trừ mana theo tổng stamina đã tiêu thụ thực tế ──
        boolean injuryRisk = false;
        if (s.getWorkoutPlan() != null) {
            int totalConsumed = logs.stream()
                    .mapToInt(l -> {
                        int cost = l.getExercise().getStaminaCost() != null ? l.getExercise().getStaminaCost() : 10;
                        int pct  = l.getCompletionPercent() != null ? l.getCompletionPercent() : 0;
                        return Math.round(cost * (pct / 100f));
                    }).sum();

            injuryRisk = manaService.consumeMana(s.getWorkoutPlan(), totalConsumed);
            if (injuryRisk) {
                notifService.sendToUser(user.getId(), "⚠️ Cảnh báo chấn thương",
                        "Bạn đã tập vượt quá thể lực hiện có. Hãy cân nhắc nghỉ ngơi để tránh chấn thương.", "SYSTEM");
            }
        }

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

            // Gợi ý tăng/giảm tạ (note hiển thị) — áp dụng cho CẢ 2 loại plan
            applyWeightAdjustmentNote(user, s.getWorkoutPlan(), s.getWeekNumber());

            // ── Điều chỉnh tạ THỰC theo nhóm cơ, dựa trên completionPercent cả tuần ──
            List<SessionExerciseLog> weekLogs = logRepo.findByUserIdAndPlanIdAndWeekNumber(
                    user.getId(), s.getWorkoutPlan().getId(), s.getWeekNumber());
            adjustMuscleGroupWeights(s.getWorkoutPlan(), weekLogs);

            if (isTemplatePlan) {
                advanceTemplatePlanWeek(s.getWorkoutPlan());
            } else if (isAiPlan) {
                try {
                    workoutPlanService.adjustPlanAfterWeek(
                            s.getWorkoutPlan().getId(),
                            email,
                            req.getCheckoutWeight(),
                            req.getCheckoutBodyFat()
                    );
                } catch (Exception e) {
                    notifService.sendToUser(user.getId(),
                            "⚠️ Lưu ý",
                            "Buổi tập đã hoàn thành nhưng căn chỉnh tuần mới gặp sự cố. Vui lòng thử lại.",
                            "SYSTEM");
                }
            }
        }

        // Thông báo kết quả
        String msg = sessionCompletionRate >= 90
                ? "🔥 Xuất sắc! " + sessionCompletionRate + "% hoàn thành!"
                : sessionCompletionRate >= 70
                  ? "✅ Tốt! " + sessionCompletionRate + "% hoàn thành."
                  : "💪 " + sessionCompletionRate + "% — cố gắng hơn buổi sau nhé!";
        notifService.sendToUser(user.getId(), "Kết quả buổi tập", msg, "SYSTEM");

        if (Boolean.TRUE.equals(s.getIsLastSessionOfWeek())) {
            String nextMsg = isTemplatePlan
                    ? "Dữ liệu đã ghi nhận. Giáo án tự động chuyển sang tuần tiếp theo."
                    : "Dữ liệu đã ghi nhận và giáo án đã được căn chỉnh cho tuần mới.";
            notifService.sendToUser(user.getId(),
                    "📊 Hoàn thành tuần " + s.getWeekNumber() + "!",
                    nextMsg, "SYSTEM");
        }

        try { petService.recalculate(email); } catch (Exception ignored) {}

        WorkoutSessionResponse resp = buildResponse(s);
        resp.setInjuryRisk(injuryRisk);
        return resp;
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
                        .isCompleted(l.getIsCompleted())
                        .completionPercent(l.getCompletionPercent())
                        .notes(l.getNotes()).build()
        ).collect(Collectors.toList());

        List<WorkoutPlanExerciseResponse> planExs = Collections.emptyList();
        if (s.getPlanDay() != null && s.getPlanDay().getExercises() != null) {
            planExs = s.getPlanDay().getExercises().stream().map(pe -> {
                boolean justRevealed = pe.getWeightUpdatedWeek() != null
                        && pe.getPlanDay() != null && pe.getPlanDay().getWorkoutPlan() != null
                        && pe.getWeightUpdatedWeek().equals(pe.getPlanDay().getWorkoutPlan().getCurrentWeek());
                return WorkoutPlanExerciseResponse.builder()
                        .id(pe.getId()).exerciseId(pe.getExercise().getId())
                        .exerciseName(pe.getExercise().getName())
                        .muscleGroup(pe.getExercise().getMuscleGroup()!=null ? pe.getExercise().getMuscleGroup().name() : null)
                        .difficulty(pe.getExercise().getDifficulty()!=null   ? pe.getExercise().getDifficulty().name()  : null)
                        .sets(pe.getSets())
                        .reps(pe.getReps())
                        .durationSeconds(pe.getDurationSeconds())
                        .restSeconds(pe.getRestSeconds()).orderIndex(pe.getOrderIndex())
                        .notes(pe.getNotes())
                        .videoUrl(pe.getExercise().getVideoUrl())
                        .caloriesBurned(pe.getExercise().getCaloriesBurned())
                        .baseWeightKg(pe.getBaseWeightKg())
                        .currentWeightKg(pe.getCurrentWeightKg())
                        .weightJustRevealed(justRevealed)
                        .build();
            }).collect(Collectors.toList());
        }

        ScheduleCheckInfo scheduleInfo = buildScheduleCheckInfo(s);

        return WorkoutSessionResponse.builder()
                .id(s.getId()).sessionDate(s.getSessionDate()).scheduledTime(s.getScheduledTime())
                .checkInTime(s.getCheckInTime()).checkOutTime(s.getCheckOutTime())
                .status(s.getStatus()).totalCaloriesBurned(s.getTotalCaloriesBurned())
                .durationMinutes(s.getDurationMinutes()).notes(s.getNotes())
                .weekNumber(s.getWeekNumber())
                .planId(s.getWorkoutPlan() != null ? s.getWorkoutPlan().getId() : null)
                .planName(s.getWorkoutPlan()!=null ? s.getWorkoutPlan().getPlanName() : null)
                .dayName(s.getPlanDay()!=null       ? s.getPlanDay().getDayName()    : null)
                .customSessionName(s.getCustomSessionName()).isCustom(s.getIsCustom())
                .completionRate(s.getCompletionRate())
                .isLastSessionOfWeek(s.getIsLastSessionOfWeek())
                .checkoutWeight(s.getCheckoutWeight()).checkoutBodyFat(s.getCheckoutBodyFat())
                .exerciseLogs(logs).planExercises(planExs)
                .dayMismatchWarning(scheduleInfo != null ? scheduleInfo.warning() : null)
                .scheduleSelectionRequired(scheduleInfo != null && scheduleInfo.selectionRequired())
                .scheduleOptions(scheduleInfo != null ? scheduleInfo.options() : null)
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

    // ─────────────────────────────────────────────────────────
    // Lập lịch tập — Day Mismatch Detection (mục 8.3 I.docx)
    // ─────────────────────────────────────────────────────────
    private record ScheduleCheckInfo(String warning, boolean selectionRequired, List<List<Integer>> options) {}

    /**
     * - Giáo án mẫu (template): 1 lịch cố định lấy từ planDays, giữ nguyên logic cũ.
     * - Giáo án AI:
     *     + Nếu plan.confirmedScheduleDows đã có (do người dùng CHỦ ĐỘNG chọn lại sau
     *       khi hệ thống không xác định được lịch) -> dùng lịch đó.
     *     + Ngược lại: chạy thuật toán loại trừ dần trên TOÀN BỘ lịch sử check-in mỗi
     *       lần gọi (KHÔNG lưu DB khi tự xác định được):
     *         survivors == 1  -> coi là lịch chuẩn, kiểm tra mismatch bình thường.
     *         survivors  > 1  -> chưa đủ dữ liệu, chưa cảnh báo gì, tiếp tục theo dõi.
     *         survivors == 0  -> không còn lịch nào phù hợp, yêu cầu người dùng chọn lại
     *                            (selectionRequired=true, options=toàn bộ candidate).
     */
    private ScheduleCheckInfo buildScheduleCheckInfo(WorkoutSession s) {
        WorkoutPlan plan = s.getWorkoutPlan();
        if (plan == null || s.getSessionDate() == null) return null;

        boolean isAiPlan = Boolean.TRUE.equals(plan.getIsAiGenerated());
        boolean isTemplatePlan = Boolean.FALSE.equals(plan.getIsAiGenerated());
        if (!isAiPlan && !isTemplatePlan) return null;

        List<WorkoutSession> allSessions = sessionRepo.findByPlanOrderBySessionDate(s.getUser().getId(), plan.getId());
        if (allSessions.isEmpty()) return null;

        List<Integer> schedule;

        if (isTemplatePlan) {
            List<WorkoutPlanDay> planDays = plan.getPlanDays() != null
                    ? plan.getPlanDays()
                    : dayRepo.findByWorkoutPlanIdOrderByDayOfWeek(plan.getId());
            if (planDays == null || planDays.isEmpty()) return null;
            schedule = planDays.stream()
                    .map(WorkoutPlanDay::getDayOfWeek)
                    .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
            if (schedule.isEmpty()) return null;
        } else {
            if (plan.getConfirmedScheduleDows() != null) {
                schedule = ScheduleCatalog.parse(plan.getConfirmedScheduleDows());
            } else {
                List<List<Integer>> candidates = ScheduleCatalog.candidatesFor(plan.getSessionsPerWeek());
                List<List<Integer>> survivors = eliminateCandidates(candidates, allSessions);

                if (survivors.isEmpty()) {
                    return new ScheduleCheckInfo(
                            "Không thể xác định lịch tập chuẩn từ các buổi tập hiện tại. " +
                                    "Vui lòng chọn một trong các lịch tập khuyến nghị để hệ thống tiếp tục theo dõi chu kỳ tập luyện.",
                            true,
                            candidates);
                } else if (survivors.size() > 1) {
                    // chưa đủ dữ liệu để chốt lịch chuẩn -> chưa cảnh báo, tiếp tục theo dõi
                    return new ScheduleCheckInfo(null, false, null);
                } else {
                    // survivors == 1 -> tự động coi là lịch chuẩn, KHÔNG lưu DB, suy luận lại mỗi lần gọi
                    schedule = survivors.get(0);
                }
            }
        }

        int anchorDow = allSessions.get(0).getSessionDate().getDayOfWeek().getValue();
        int idxInSchedule = schedule.indexOf(anchorDow);
        List<Integer> rotated = idxInSchedule >= 0 ? rotate(schedule, idxInSchedule) : schedule;

        int indexOfCurrent = -1;
        for (int i = 0; i < allSessions.size(); i++) {
            if (allSessions.get(i).getId().equals(s.getId())) {
                indexOfCurrent = i;
                break;
            }
        }
        if (indexOfCurrent < 0) return null;

        int expectedDow = rotated.get(indexOfCurrent % rotated.size());
        int actualDow = s.getSessionDate().getDayOfWeek().getValue();

        String warning = null;
        if (actualDow != expectedDow) {
            warning = "⚠️ Theo chu kỳ tập của bạn, buổi này nên rơi vào " + dowVietnameseName(expectedDow)
                    + " nhưng bạn đang tập vào " + dowVietnameseName(actualDow)
                    + ". Tập không đúng chu kỳ có thể làm giáo án không đạt hiệu quả tối ưu.";
        }
        return new ScheduleCheckInfo(warning, false, null);
    }

    /** Loại trừ dần: giữ lại các candidate mà TOÀN BỘ lịch sử check-in khớp, sau khi xoay theo buổi đầu tiên. */
    private List<List<Integer>> eliminateCandidates(List<List<Integer>> candidates, List<WorkoutSession> allSessions) {
        int anchorDow = allSessions.get(0).getSessionDate().getDayOfWeek().getValue();
        List<List<Integer>> survivors = new ArrayList<>();
        for (List<Integer> candidate : candidates) {
            int idx = candidate.indexOf(anchorDow);
            if (idx < 0) continue; // ngày bắt đầu không thuộc lịch này -> loại
            List<Integer> rotated = rotate(candidate, idx);
            boolean ok = true;
            for (int i = 0; i < allSessions.size(); i++) {
                int expected = rotated.get(i % rotated.size());
                int actual = allSessions.get(i).getSessionDate().getDayOfWeek().getValue();
                if (expected != actual) { ok = false; break; }
            }
            if (ok) survivors.add(rotated);
        }
        return survivors;
    }

    private List<Integer> rotate(List<Integer> list, int startIdx) {
        List<Integer> rotated = new ArrayList<>();
        int n = list.size();
        for (int i = 0; i < n; i++) rotated.add(list.get((startIdx + i) % n));
        return rotated;
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

    // ── Điều chỉnh tạ THỰC theo nhóm cơ (tích lũy multiplier) ──
    private void adjustMuscleGroupWeights(WorkoutPlan plan, List<SessionExerciseLog> weekLogs) {
        Map<MuscleGroup, List<Integer>> byGroup = weekLogs.stream()
                .filter(l -> l.getCompletionPercent() != null && l.getExercise().getMuscleGroup() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getExercise().getMuscleGroup(),
                        Collectors.mapping(SessionExerciseLog::getCompletionPercent, Collectors.toList())));

        int nextWeek = (plan.getCurrentWeek() != null ? plan.getCurrentWeek() : 1) + 1;

        for (var entry : byGroup.entrySet()) {
            MuscleGroup mg = entry.getKey();
            double avgRate = entry.getValue().stream().mapToInt(i -> i).average().orElse(0);

            double factor = avgRate >= 90 ? 1.10
                    : avgRate >= 80 ? 1.05
                      : avgRate >= 70 ? 1.00
                        : avgRate >= 60 ? 0.95
                          : 0.90;

            WorkoutPlanMuscleGroupWeight mgw = mgWeightRepo
                    .findByWorkoutPlanIdAndMuscleGroup(plan.getId(), mg)
                    .orElseGet(() -> WorkoutPlanMuscleGroupWeight.builder()
                            .workoutPlan(plan).muscleGroup(mg).multiplier(1.0).build());
            mgw.setMultiplier(mgw.getMultiplier() * factor);
            mgWeightRepo.save(mgw);

            List<WorkoutPlanExercise> exs = planExerciseRepo
                    .findByPlanDay_WorkoutPlan_IdAndExercise_MuscleGroup(plan.getId(), mg);
            for (WorkoutPlanExercise pe : exs) {
                if (pe.getBaseWeightKg() != null) {
                    pe.setCurrentWeightKg(Math.round(pe.getBaseWeightKg() * mgw.getMultiplier() * 10.0) / 10.0);
                    pe.setWeightUpdatedWeek(nextWeek);
                }
            }
            planExerciseRepo.saveAll(exs);
        }
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