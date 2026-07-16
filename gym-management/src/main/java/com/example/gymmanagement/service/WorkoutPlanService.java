package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.TemplateDayRequest;
import com.example.gymmanagement.dto.request.TemplateExerciseRequest;
import com.example.gymmanagement.dto.request.WorkoutPlanRequest;
import com.example.gymmanagement.dto.request.WorkoutTemplateRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.*;
import com.example.gymmanagement.repository.*;
import com.example.gymmanagement.service.plan.MuscleGroupSplitPlanner;
import com.example.gymmanagement.service.schedule.ScheduleCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutPlanService {

    private final WorkoutPlanRepository     planRepo;
    private final WorkoutPlanDayRepository  dayRepo;
    private final UserRepository            userRepo;
    private final ExerciseRepository        exerciseRepo;
    private final UserProfileRepository     profileRepo;
    private final WorkoutSessionRepository  sessionRepo;
    private final FitnessCalculator fitnessCalculator;
    private final WorkoutPlanExerciseRepository planExerciseRepo;
    private final MembershipRepository membershipRepo;
    // ── MỚI (Patch 7) ──
    private final EnduranceTestRepository enduranceTestRepo;
    private final EstimatedWeeksCalculator estimatedWeeksCalculator;

    private static final int FREE_PLAN_LIMIT_PER_MONTH = 1;

    private void checkPlanGenerationLimit(User user) {
        boolean isVip = membershipRepo.findByUserIdAndIsActiveTrue(user.getId())
                .map(m -> m.getMembershipType() == MembershipType.VIP)
                .orElse(false);
        if (isVip) return;

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        long countThisMonth = planRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsTemplate()))
                .filter(p -> p.getCreatedAt() != null && !p.getCreatedAt().toLocalDate().isBefore(monthStart))
                .count();

        if (countThisMonth >= FREE_PLAN_LIMIT_PER_MONTH) {
            throw new RuntimeException("Gói Free chỉ được tạo/đổi giáo án " + FREE_PLAN_LIMIT_PER_MONTH +
                    " lần/tháng. Nâng cấp lên gói VIP để tạo giáo án không giới hạn.");
        }
    }

    // ─────────────────────────────────────────────────────────
    // 1. generateAIPlanWithGoal — FS + BodyType + Level + Goal + Mana + Target (Patch 7)
    // ─────────────────────────────────────────────────────────
    @Transactional
    public WorkoutPlanResponse generateAIPlanWithGoal(String email, Goal goal,
                                                      FitnessLevel levelParam, Integer daysPerWeek,
                                                      Double targetDeltaKg,
                                                      String enduranceMetric,
                                                      Double enduranceTargetValue) {
        User user = getUser(email);
        UserProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);

        FitnessLevel level = levelParam != null ? levelParam
                : (profile != null && profile.getFitnessLevel() != null
                   ? profile.getFitnessLevel() : FitnessLevel.BEGINNER);

        daysPerWeek = calcSessionsPerWeek(goal, daysPerWeek, profile);

        Double startBmi    = profile != null ? profile.getBmi()    : null;
        Double startWeight = profile != null ? profile.getWeight() : null;

        double fs = (profile != null && profile.getAge() != null
                && profile.getHeight() != null && profile.getWeight() != null)
                ? fitnessCalculator.calculateFS(profile.getAge(), profile.getHeight(),
                profile.getWeight(), profile.getGender())
                : 60.0;

        FitnessCalculator.FsLevel fsLevel = fitnessCalculator.getFsLevel(fs);

        FitnessCalculator.BodyType bodyType = (profile != null)
                ? fitnessCalculator.classifyBodyType(profile.getHeight(), profile.getWeight(), startBmi,
                profile.getGender(), profile.getBodyFatPercentage())
                : FitnessCalculator.BodyType.CAN_DOI;

        // ── MỚI (Patch 7): xác định target theo Goal — Business Rules v2 (LOCKED).
        // Validate TRƯỚC deactivateAndCleanOldPlan() để tránh huỷ giáo án cũ nếu request lỗi. ──
        String targetMetricTypeVal = null;
        Double targetBaselineValueVal = null;
        Double targetGoalValueVal = null;
        Double targetCurrentValueVal = null;

        switch (goal) {
            case MUSCLE_GAIN -> {
                if (targetDeltaKg == null || targetDeltaKg <= 0)
                    throw new RuntimeException("Mục tiêu Tăng cơ yêu cầu targetDeltaKg > 0");
                if (startWeight == null)
                    throw new RuntimeException("Cần có cân nặng trong hồ sơ để tạo giáo án theo mục tiêu này");
                targetBaselineValueVal = startWeight;
                targetGoalValueVal = startWeight + targetDeltaKg;
                targetCurrentValueVal = startWeight;
            }
            case WEIGHT_LOSS -> {
                if (targetDeltaKg == null || targetDeltaKg >= 0)
                    throw new RuntimeException("Mục tiêu Giảm cân yêu cầu targetDeltaKg < 0");
                if (startWeight == null)
                    throw new RuntimeException("Cần có cân nặng trong hồ sơ để tạo giáo án theo mục tiêu này");
                targetBaselineValueVal = startWeight;
                targetGoalValueVal = startWeight + targetDeltaKg;
                targetCurrentValueVal = startWeight;
            }
            case ENDURANCE -> {
                if (enduranceMetric == null || enduranceTargetValue == null)
                    throw new RuntimeException("Mục tiêu Sức bền yêu cầu enduranceMetric và enduranceTargetValue");
                if (!List.of("PUSHUP_REPS", "PLANK_SECONDS", "SQUAT_REPS").contains(enduranceMetric))
                    throw new RuntimeException("enduranceMetric không hợp lệ: " + enduranceMetric);

                EnduranceTest test = enduranceTestRepo.findByUserId(user.getId())
                        .orElseThrow(() -> new RuntimeException(
                                "Bạn cần thực hiện bài test sức bền trước khi tạo giáo án cho mục tiêu này"));

                Double baseline = switch (enduranceMetric) {
                    case "PUSHUP_REPS" -> test.getPushupReps() != null ? test.getPushupReps().doubleValue() : null;
                    case "PLANK_SECONDS" -> test.getPlankSeconds() != null ? test.getPlankSeconds().doubleValue() : null;
                    case "SQUAT_REPS" -> test.getSquatReps() != null ? test.getSquatReps().doubleValue() : null;
                    default -> null;
                };
                if (baseline == null)
                    throw new RuntimeException("Kết quả bài test cho " + enduranceMetric + " chưa có dữ liệu");

                targetMetricTypeVal = enduranceMetric;
                targetBaselineValueVal = baseline;
                targetGoalValueVal = enduranceTargetValue;
                // targetCurrentValue KHÔNG lưu cứng cho ENDURANCE — luôn đọc live từ EnduranceTest (LOCKED)
                targetCurrentValueVal = null;
            }
            case MAINTENANCE -> {
                // Không có targetGoalValue (LOCKED) — chỉ cần baseline để Patch 9 so sánh độ lệch.
                targetBaselineValueVal = startWeight;
                targetGoalValueVal = null;
                targetCurrentValueVal = startWeight;
            }
        }

        // ── MỚI (Patch 7): estimatedWeeks — tính qua module độc lập, KHÔNG hardcode ──
        int estimatedWeeks = estimatedWeeksCalculator.calculate(
                goal, level, targetDeltaKg,
                goal == Goal.ENDURANCE ? targetBaselineValueVal : null,
                goal == Goal.ENDURANCE ? targetGoalValueVal : null);

        deactivateAndCleanOldPlan(user.getId());

        int maxMana = (int) Math.round(fs * 2);

        WorkoutPlan plan = WorkoutPlan.builder()
                .user(user)
                .planName(buildPlanName(goal, level))
                .description(buildPlanDesc(goal, level, daysPerWeek, profile))
                .goal(goal).targetLevel(level)
                .durationWeeks(estimatedWeeks)
                .estimatedWeeks(estimatedWeeks)
                .sessionsPerWeek(daysPerWeek).currentWeek(1)
                .startingBmi(startBmi).startingWeight(startWeight)
                .isActive(true).isAiGenerated(true)
                .maxMana(maxMana)
                .currentMana(maxMana)
                .fitnessScore((int) Math.round(fs))
                .fitnessLevel(fsLevel)
                .bodyType(bodyType)
                .targetMetricType(targetMetricTypeVal)
                .targetBaselineValue(targetBaselineValueVal)
                .targetGoalValue(targetGoalValueVal)
                .targetCurrentValue(targetCurrentValueVal)
                .targetAchieved(false)
                .build();
        planRepo.save(plan);

        List<WorkoutPlanDay> days = buildPlanDaysNew(plan, goal, level, fsLevel, bodyType, daysPerWeek, profile, fs);
        dayRepo.saveAll(days);
        plan.setPlanDays(days);

        return toPlanResponse(plan, profile);
    }

    // ─────────────────────────────────────────────────────────
    // 2. Lấy giáo án
    // ─────────────────────────────────────────────────────────
    public WorkoutPlanResponse getActivePlan(String email) {
        User user = getUser(email);
        WorkoutPlan plan = planRepo.findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new RuntimeException("Chưa có giáo án active."));
        plan.setPlanDays(dayRepo.findByWorkoutPlanIdOrderByDayOfWeek(plan.getId()));
        UserProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);
        return toPlanResponse(plan, profile);
    }

    public List<WorkoutPlanResponse> getAllPlans(String email) {
        User user = getUser(email);
        UserProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);
        return planRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(p -> {
            p.setPlanDays(dayRepo.findByWorkoutPlanIdOrderByDayOfWeek(p.getId()));
            return toPlanResponse(p, profile);
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────
    // 6. adjustPlanAfterWeek — giữ nguyên (Patch 8 sẽ sửa)
    // ─────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────
    // 6. adjustPlanAfterWeek — Patch 8: Business Rules v2 (chỉ áp dụng khi estimatedWeeks != null)
    // ─────────────────────────────────────────────────────────
    private static final double PROGRESS_TOLERANCE_PERCENT = 5.0;
    private static final int MIN_DURATION_WEEKS = 1;
    private static final int MAX_DURATION_WEEKS = 50;
    private static final String MAX_DURATION_END_MESSAGE =
            "Bạn đã hoàn thành thời lượng tối đa của giáo án nhưng chưa đạt mục tiêu. " +
                    "Hãy cân nhắc đánh giá lại tình trạng sức khỏe và tham khảo huấn luyện viên hoặc chuyên gia " +
                    "nếu cần trước khi tiếp tục với một giáo án mới.";

    @Transactional
    public WorkoutPlanResponse adjustPlanAfterWeek(Long planId, String email,
                                                   Double newWeight, Double newBodyFat) {
        User user = getUser(email);
        WorkoutPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        int week = plan.getCurrentWeek() != null ? plan.getCurrentWeek() : 1;

        if (newWeight != null) {
            plan.setStartingWeight(newWeight);
            profileRepo.findByUserId(user.getId()).ifPresent(profile -> {
                profile.setWeight(newWeight);
                if (profile.getHeight() != null && profile.getHeight() > 0) {
                    double hM  = profile.getHeight() / 100.0;
                    double bmi = Math.round(newWeight / (hM * hM) * 10.0) / 10.0;
                    profile.setBmi(bmi);
                    plan.setStartingBmi(bmi);
                }
                if (newBodyFat != null) profile.setBodyFatPercentage(newBodyFat);
                profileRepo.save(profile);
            });
        }

        String note = null;
        boolean endPlanNow = false;

        if (plan.getEstimatedWeeks() == null) {
            // ── Giáo án cũ (tạo trước Patch 7) — chạy nguyên logic cũ (LOCKED) ──
            long completed = sessionRepo.countCompletedInWeek(user.getId(), planId, week);
            int  target    = plan.getSessionsPerWeek();
            if (completed < target) {
                plan.setDurationWeeks(plan.getDurationWeeks() + 1);
                note = "📅 Bạn bỏ " + (target - completed) + " buổi tuần này. Đã gia hạn thêm 1 tuần.";
            }
        } else {
            // ── Business Rules v2 ──
            Double current = resolveCurrentTargetValue(plan, newWeight);

            // ENDURANCE không ghi targetCurrentValue vào WorkoutPlan (LOCKED)
            if (plan.getGoal() != Goal.ENDURANCE && current != null) {
                plan.setTargetCurrentValue(current);
            }

            if (plan.getGoal() == Goal.MAINTENANCE) {
                // ── Chỉ chuẩn bị dữ liệu cho Patch 9 — không đánh giá đạt/chưa đạt,
                // không điều chỉnh durationWeeks (không có targetGoalValue để so sánh) ──
            } else if (plan.getTargetBaselineValue() != null && plan.getTargetGoalValue() != null && current != null) {
                double baseline = plan.getTargetBaselineValue();
                double goalVal = plan.getTargetGoalValue();
                Boolean achieved = checkAchieved(plan.getGoal(), baseline, goalVal, current);

                if (Boolean.TRUE.equals(achieved)) {
                    plan.setTargetAchieved(true);
                    plan.setIsCompleted(true);
                    plan.setIsActive(false);
                    endPlanNow = true;
                    note = "🎉 Chúc mừng! Bạn đã đạt mục tiêu của giáo án.";
                } else {
                    Double percentGoal = computePercentGoal(plan.getGoal(), baseline, goalVal, current);
                    if (percentGoal != null) {
                        double percentTime = (double) week / plan.getEstimatedWeeks() * 100;
                        double gap = percentGoal - percentTime;

                        int newDuration = plan.getDurationWeeks();
                        if (gap > PROGRESS_TOLERANCE_PERCENT) {
                            newDuration -= 1;
                            note = "🚀 Tiến độ nhanh hơn dự kiến. Đã rút ngắn 1 tuần.";
                        } else if (gap < -PROGRESS_TOLERANCE_PERCENT) {
                            newDuration += 1;
                            note = "🐢 Tiến độ chậm hơn dự kiến. Đã gia hạn thêm 1 tuần.";
                        }
                        newDuration = Math.max(MIN_DURATION_WEEKS, Math.min(MAX_DURATION_WEEKS, newDuration));
                        plan.setDurationWeeks(newDuration);

                        if (newDuration >= MAX_DURATION_WEEKS) {
                            plan.setIsCompleted(true);
                            plan.setIsActive(false);
                            endPlanNow = true;
                            note = MAX_DURATION_END_MESSAGE;
                        }
                    }
                }
            }
        }

        plan.setCurrentWeek(week + 1);

        if (!endPlanNow && plan.getCurrentWeek() > plan.getDurationWeeks()) {
            plan.setIsCompleted(true);
            plan.setIsActive(false);
        }

        plan.setPlanDays(dayRepo.findByWorkoutPlanIdOrderByDayOfWeek(planId));
        WorkoutPlan saved = planRepo.save(plan);

        WorkoutPlanResponse resp = toPlanResponse(saved,
                profileRepo.findByUserId(user.getId()).orElse(null));
        if (note != null) resp.setScheduleNote(note);
        return resp;
    }

    /** Lấy giá trị "current" theo Goal — MUSCLE_GAIN/WEIGHT_LOSS/MAINTENANCE dùng newWeight
     *  (fallback về targetCurrentValue cũ nếu không nhập); ENDURANCE đọc LIVE từ EnduranceTest. */
    private Double resolveCurrentTargetValue(WorkoutPlan plan, Double newWeight) {
        return switch (plan.getGoal()) {
            case MUSCLE_GAIN, WEIGHT_LOSS, MAINTENANCE ->
                    newWeight != null ? newWeight : plan.getTargetCurrentValue();
            case ENDURANCE -> readLiveEnduranceValue(plan);
        };
    }

    private Double readLiveEnduranceValue(WorkoutPlan plan) {
        if (plan.getUser() == null || plan.getTargetMetricType() == null) return null;
        String metricType = plan.getTargetMetricType();
        return enduranceTestRepo.findByUserId(plan.getUser().getId())
                .map(test -> switch (metricType) {
                    case "PUSHUP_REPS" -> test.getPushupReps() != null ? test.getPushupReps().doubleValue() : null;
                    case "PLANK_SECONDS" -> test.getPlankSeconds() != null ? test.getPlankSeconds().doubleValue() : null;
                    case "SQUAT_REPS" -> test.getSquatReps() != null ? test.getSquatReps().doubleValue() : null;
                    default -> null;
                })
                .orElse(null);
    }

    /** Ngưỡng đạt mục tiêu — LOCKED cho toàn bộ Goal có targetGoalValue (MUSCLE_GAIN,
     *  WEIGHT_LOSS, ENDURANCE dùng chung công thức baseline + (goal-baseline)×95%,
     *  đạt khi current >= threshold). WEIGHT_LOSS đảo chiều. MAINTENANCE không áp dụng. */
    private Boolean checkAchieved(Goal goal, double baseline, double goalVal, double current) {
        return switch (goal) {
            case MUSCLE_GAIN, ENDURANCE -> current >= (baseline + (goalVal - baseline) * 0.95);
            case WEIGHT_LOSS -> current <= (baseline - (baseline - goalVal) * 0.95);
            default -> null;
        };
    }

    /** %mục tiêu đã hoàn thành — dùng để so với %thời gian đã dùng (currentWeek/estimatedWeeks). */
    private Double computePercentGoal(Goal goal, double baseline, double goalVal, double current) {
        return switch (goal) {
            case MUSCLE_GAIN, ENDURANCE ->
                    (goalVal - baseline) != 0 ? (current - baseline) / (goalVal - baseline) * 100 : null;
            case WEIGHT_LOSS ->
                    (baseline - goalVal) != 0 ? (baseline - current) / (baseline - goalVal) * 100 : null;
            default -> null;
        };
    }




    @Transactional
    public WorkoutPlanExerciseResponse setBaseWeight(Long planExerciseId, Double weight) {
        WorkoutPlanExercise pe = planExerciseRepo.findById(planExerciseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập trong giáo án"));
        if (pe.getBaseWeightKg() != null)
            throw new RuntimeException("Tạ khởi điểm đã được thiết lập, không thể sửa lại.");
        if (weight == null || weight < 0)
            throw new RuntimeException("Giá trị tạ không hợp lệ");

        pe.setBaseWeightKg(weight);
        pe.setCurrentWeightKg(weight);
        planExerciseRepo.save(pe);
        return buildExResponse(pe);
    }

    @Transactional
    public WorkoutPlanResponse confirmSchedule(String email, Long planId, List<Integer> dayOfWeek) {
        User user = getUser(email);
        WorkoutPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        if (plan.getUser() == null || !plan.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Access denied");
        if (plan.getSessionsPerWeek() == null)
            throw new RuntimeException("Giáo án chưa có sessionsPerWeek hợp lệ");

        List<Integer> matched = ScheduleCatalog.matchCandidate(plan.getSessionsPerWeek(), dayOfWeek)
                .orElseThrow(() -> new RuntimeException(
                        "Lịch tập không hợp lệ với số buổi/tuần hiện tại (" + plan.getSessionsPerWeek() + " buổi)"));

        plan.setConfirmedScheduleDows(ScheduleCatalog.format(matched));
        planRepo.save(plan);

        plan.setPlanDays(dayRepo.findByWorkoutPlanIdOrderByDayOfWeek(planId));
        return toPlanResponse(plan, profileRepo.findByUserId(user.getId()).orElse(null));
    }

    @Transactional
    public WorkoutPlanResponse createManualTemplate(WorkoutTemplateRequest req) {
        validateTemplateRequest(req);

        WorkoutPlan plan = WorkoutPlan.builder()
                .user(null)
                .planName(req.getPlanName())
                .description(req.getDescription())
                .goal(req.getGoal())
                .targetLevel(req.getTargetLevel())
                .durationWeeks(req.getDurationWeeks())
                .sessionsPerWeek(req.getDays().size())
                .currentWeek(1)
                .isActive(true)
                .isAiGenerated(false)
                .isTemplate(true)
                .build();
        planRepo.save(plan);

        List<WorkoutPlanDay> days = buildDaysFromRequest(plan, req.getDays());
        dayRepo.saveAll(days);
        plan.setPlanDays(days);

        return toPlanResponse(plan, null);
    }

    @Transactional
    public WorkoutPlanResponse updateManualTemplate(Long templateId, WorkoutTemplateRequest req) {
        validateTemplateRequest(req);

        WorkoutPlan plan = planRepo.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        if (!Boolean.TRUE.equals(plan.getIsTemplate())) {
            throw new RuntimeException("Plan này không phải template");
        }

        plan.setPlanName(req.getPlanName());
        plan.setDescription(req.getDescription());
        plan.setGoal(req.getGoal());
        plan.setTargetLevel(req.getTargetLevel());
        plan.setDurationWeeks(req.getDurationWeeks());
        plan.setSessionsPerWeek(req.getDays().size());

        List<WorkoutPlanDay> oldDays = dayRepo.findByWorkoutPlanIdOrderByDayOfWeek(plan.getId());
        if (oldDays != null && !oldDays.isEmpty()) {
            sessionRepo.deleteByPlanDayIds(oldDays.stream().map(WorkoutPlanDay::getId).collect(Collectors.toList()));
            dayRepo.deleteAll(oldDays);
        }

        List<WorkoutPlanDay> newDays = buildDaysFromRequest(plan, req.getDays());
        List<WorkoutPlanDay> saved = dayRepo.saveAll(newDays);
        plan.setPlanDays(saved);

        WorkoutPlan savedPlan = planRepo.save(plan);
        return toPlanResponse(savedPlan, null);
    }

    public List<WorkoutPlanResponse> getAllTemplates(boolean onlyActive) {
        List<WorkoutPlan> templates = onlyActive
                ? planRepo.findByIsTemplateTrueAndIsActiveTrueOrderByCreatedAtDesc()
                : planRepo.findByIsTemplateTrueOrderByCreatedAtDesc();
        return templates.stream().map(p -> {
            p.setPlanDays(dayRepo.findByWorkoutPlanIdOrderByDayOfWeek(p.getId()));
            return toPlanResponse(p, null);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        WorkoutPlan plan = planRepo.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        if (!Boolean.TRUE.equals(plan.getIsTemplate())) {
            throw new RuntimeException("Plan này không phải template");
        }
        plan.setIsActive(false);
        planRepo.save(plan);
    }

    @Transactional
    public WorkoutPlanResponse selectTemplate(String email, Long templateId) {
        User user = getUser(email);
        checkPlanGenerationLimit(user);
        WorkoutPlan template = planRepo.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        if (!Boolean.TRUE.equals(template.getIsTemplate())) {
            throw new RuntimeException("Plan này không phải template");
        }
        List<WorkoutPlanDay> templateDays = dayRepo.findByWorkoutPlanIdOrderByDayOfWeek(template.getId());
        UserProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);

        deactivateAndCleanOldPlan(user.getId());

        WorkoutPlan newPlan = WorkoutPlan.builder()
                .user(user)
                .planName(template.getPlanName())
                .description(template.getDescription())
                .goal(template.getGoal())
                .targetLevel(template.getTargetLevel())
                .durationWeeks(template.getDurationWeeks())
                .sessionsPerWeek(template.getSessionsPerWeek())
                .currentWeek(1)
                .startingBmi(profile != null ? profile.getBmi() : null)
                .startingWeight(profile != null ? profile.getWeight() : null)
                .isActive(true)
                .isAiGenerated(false)
                .isTemplate(false)
                .build();
        planRepo.save(newPlan);

        List<WorkoutPlanDay> copiedDays = new ArrayList<>();
        for (WorkoutPlanDay srcDay : templateDays) {
            WorkoutPlanDay newDay = WorkoutPlanDay.builder()
                    .workoutPlan(newPlan)
                    .dayOfWeek(srcDay.getDayOfWeek())
                    .dayName(srcDay.getDayName())
                    .build();

            List<WorkoutPlanExercise> copiedExercises = new ArrayList<>();
            if (srcDay.getExercises() != null) {
                for (WorkoutPlanExercise srcEx : srcDay.getExercises()) {
                    copiedExercises.add(WorkoutPlanExercise.builder()
                            .planDay(newDay)
                            .exercise(srcEx.getExercise())
                            .sets(srcEx.getSets())
                            .reps(srcEx.getReps())
                            .durationSeconds(srcEx.getDurationSeconds())
                            .restSeconds(srcEx.getRestSeconds())
                            .orderIndex(srcEx.getOrderIndex())
                            .notes(srcEx.getNotes())
                            .build());
                }
            }
            newDay.setExercises(copiedExercises);
            copiedDays.add(newDay);
        }

        List<WorkoutPlanDay> savedDays = dayRepo.saveAll(copiedDays);
        newPlan.setPlanDays(savedDays);

        return toPlanResponse(newPlan, profile);
    }

    private void validateTemplateRequest(WorkoutTemplateRequest req) {
        if (req.getDays() == null || req.getDays().isEmpty()) {
            throw new RuntimeException("Template cần ít nhất 1 ngày tập");
        }
        for (TemplateDayRequest d : req.getDays()) {
            if (d.getExercises() == null || d.getExercises().isEmpty()) {
                throw new RuntimeException("Mỗi ngày tập cần ít nhất 1 bài tập (ngày: " + d.getDayName() + ")");
            }
        }
    }

    private List<WorkoutPlanDay> buildDaysFromRequest(WorkoutPlan plan, List<TemplateDayRequest> dayReqs) {
        List<WorkoutPlanDay> days = new ArrayList<>();
        for (TemplateDayRequest dReq : dayReqs) {
            WorkoutPlanDay day = WorkoutPlanDay.builder()
                    .workoutPlan(plan)
                    .dayOfWeek(dReq.getDayOfWeek())
                    .dayName(dReq.getDayName())
                    .build();

            List<WorkoutPlanExercise> exercises = new ArrayList<>();
            int idx = 1;
            for (TemplateExerciseRequest exReq : dReq.getExercises()) {
                Exercise ex = exerciseRepo.findById(exReq.getExerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise id=" + exReq.getExerciseId() + " không tồn tại"));
                exercises.add(WorkoutPlanExercise.builder()
                        .planDay(day)
                        .exercise(ex)
                        .sets(exReq.getSets())
                        .reps(exReq.getReps())
                        .durationSeconds(exReq.getDurationSeconds())
                        .restSeconds(exReq.getRestSeconds())
                        .orderIndex(exReq.getOrderIndex() != null ? exReq.getOrderIndex() : idx)
                        .notes(exReq.getNotes())
                        .build());
                idx++;
            }
            day.setExercises(exercises);
            days.add(day);
        }
        return days;
    }

    private int calcSessionsPerWeek(Goal goal, Integer requested, UserProfile profile) {
        int fromProfile = (profile != null && profile.getAvailableDaysPerWeek() != null)
                ? profile.getAvailableDaysPerWeek() : 3;
        int val = requested != null ? requested : fromProfile;

        int minRequired = switch (goal) {
            case MUSCLE_GAIN, WEIGHT_LOSS -> 4;
            case ENDURANCE, MAINTENANCE -> 3;
        };
        int maxRequired = switch (goal) {
            case MUSCLE_GAIN, WEIGHT_LOSS -> 6;
            case ENDURANCE, MAINTENANCE -> 5;
        };

        val = Math.max(val, minRequired);
        return Math.max(minRequired, Math.min(maxRequired, val));
    }

    private FitnessLevel adjustLevelByBmi(FitnessLevel level, Double bmi, Goal goal) {
        if (bmi == null) return level;
        if (goal == Goal.WEIGHT_LOSS && bmi > 30 && level == FitnessLevel.ADVANCED)
            return FitnessLevel.INTERMEDIATE;
        if (goal == Goal.WEIGHT_LOSS && bmi > 35)
            return FitnessLevel.BEGINNER;
        return level;
    }

    private List<WorkoutPlanDay> buildPlanDaysNew(WorkoutPlan plan, Goal goal,
                                                  FitnessLevel level,
                                                  FitnessCalculator.FsLevel fsLevel,
                                                  FitnessCalculator.BodyType bodyType,
                                                  int sessions,
                                                  UserProfile profile,
                                                  double fs) {
        List<Map<MuscleGroup, Integer>> weekPlan = MuscleGroupSplitPlanner.buildWeekPlan(goal, level, sessions);
        List<Integer> defaultSchedule = ScheduleCatalog.candidatesFor(sessions).get(0);
        String[] names = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        List<WorkoutPlanDay> days = new ArrayList<>();
        for (int i = 0; i < sessions; i++) {
            int dow = defaultSchedule.get(i);
            WorkoutPlanDay day = WorkoutPlanDay.builder()
                    .workoutPlan(plan)
                    .dayOfWeek(dow)
                    .dayName(names[dow])
                    .build();
            day.setExercises(buildExercisesNew(day, weekPlan.get(i), goal, level, fsLevel, bodyType, profile, fs));
            days.add(day);
        }
        return days;
    }

    private List<WorkoutPlanExercise> buildExercisesNew(WorkoutPlanDay day,
                                                        Map<MuscleGroup, Integer> groupCounts,
                                                        Goal goal,
                                                        FitnessLevel level,
                                                        FitnessCalculator.FsLevel fsLevel,
                                                        FitnessCalculator.BodyType bodyType,
                                                        UserProfile profile,
                                                        double fs) {
        var srResult = fitnessCalculator.resolveFinalSetsReps(fsLevel, goal, bodyType);
        int finalSets = srResult.sets();
        int finalReps = srResult.reps();

        List<WorkoutPlanExercise> result = new ArrayList<>();
        int idx = 1;

        for (Map.Entry<MuscleGroup, Integer> entry : groupCounts.entrySet()) {
            MuscleGroup mg = entry.getKey();
            int need = entry.getValue();
            List<Exercise> cands = getExercisesByLevelAndGoal(mg, goal, level, need);
            for (Exercise ex : cands) {
                String note = buildNote(ex, goal);
                if (srResult.loadHint() != com.example.gymmanagement.service.setrep.SetRepModels.LoadHint.NONE) {
                    String hintText = srResult.loadHint()
                            == com.example.gymmanagement.service.setrep.SetRepModels.LoadHint.INCREASE_WEIGHT
                            ? "💡 Gợi ý: tăng tạ khi thấy nhẹ"
                            : "💡 Gợi ý: giảm tạ để giữ đúng kỹ thuật";
                    note = (note == null) ? hintText : note + " — " + hintText;
                }

                Double recommendedWeightKg = computeRecommendedWeightKg(
                        mg, ex.getUsesWeight(), fs, bodyType, goal, profile);

                result.add(WorkoutPlanExercise.builder()
                        .planDay(day).exercise(ex)
                        .sets(finalSets)
                        .reps(ex.getDefaultReps() != null ? finalReps : null)
                        .durationSeconds(ex.getDefaultDurationSeconds() != null
                                ? adjustDuration(ex.getDefaultDurationSeconds(), level) : null)
                        .restSeconds(calcRest(ex.getRestSeconds(), goal))
                        .orderIndex(idx++)
                        .notes(note)
                        .recommendedWeightKg(recommendedWeightKg)
                        .build());
            }
        }
        return result;
    }

    private Double computeRecommendedWeightKg(MuscleGroup mg, Boolean usesWeight, double fs,
                                              FitnessCalculator.BodyType bodyType, Goal goal,
                                              UserProfile profile) {
        if (usesWeight == null || !usesWeight) return null;

        Double muscleFactor = muscleFactorFor(mg);
        if (muscleFactor == null) return null;

        if (profile == null || profile.getWeight() == null || profile.getHeight() == null) return null;

        double weight = profile.getWeight();
        double height = profile.getHeight();
        String gender = profile.getGender();

        double fsFactor = fsFactorFor(fs);
        double bodyTypeFactor = bodyTypeFactorFor(bodyType);
        double goalFactor = goalFactorFor(goal);

        double idealWeight = fitnessCalculator.wChuan(height, gender);
        double delta = weight - idealWeight;
        double deltaFactor = deltaFactorFor(delta);

        double raw = weight * muscleFactor * fsFactor * bodyTypeFactor * goalFactor * deltaFactor;
        return Math.round(raw * 2) / 2.0;
    }

    private Double muscleFactorFor(MuscleGroup mg) {
        return switch (mg) {
            case CHEST -> 0.40;
            case BACK -> 0.50;
            case LEGS -> 0.60;
            case SHOULDERS -> 0.25;
            case ARMS -> 0.20;
            case CORE -> 0.15;
            default -> null;
        };
    }

    private double fsFactorFor(double fs) {
        if (fs >= 90) return 1.20;
        if (fs >= 80) return 1.10;
        if (fs >= 60) return 1.00;
        if (fs >= 40) return 0.85;
        return 0.75;
    }

    private double bodyTypeFactorFor(FitnessCalculator.BodyType bt) {
        return switch (bt) {
            case CAO_GAY -> 0.85;
            case GAY_CAN_DOI -> 0.90;
            case CAN_DOI -> 1.00;
            case CO_BAP -> 1.10;
            case VAN_DONG_VIEN -> 1.20;
            case THUA_CAN -> 0.95;
        };
    }

    private double goalFactorFor(Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN -> 1.05;
            case WEIGHT_LOSS -> 0.90;
            case MAINTENANCE -> 1.00;
            case ENDURANCE -> 0.80;
        };
    }

    private double deltaFactorFor(double delta) {
        if (delta < -10) return 0.85;
        if (delta < -5) return 0.90;
        if (delta <= 5) return 1.00;
        if (delta <= 10) return 1.05;
        return 1.00;
    }

    private List<Exercise> getExercisesByLevelAndGoal(MuscleGroup mg, Goal goal,
                                                      FitnessLevel level, int need) {
        List<Exercise> result = new ArrayList<>();

        List<Difficulty> priorityOrder = switch (level) {
            case BEGINNER     -> List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD);
            case INTERMEDIATE -> List.of(Difficulty.MEDIUM, Difficulty.EASY, Difficulty.HARD);
            case ADVANCED     -> List.of(Difficulty.HARD, Difficulty.MEDIUM, Difficulty.EASY);
        };

        for (Difficulty diff : priorityOrder) {
            if (result.size() >= need) break;
            List<Exercise> pool = exerciseRepo
                    .findByMuscleGroupAndDifficultyAndIsActiveTrue(mg, diff);
            pool.sort((a, b) -> {
                int cmp = Integer.compare(getGoalScore(b, goal), getGoalScore(a, goal));
                return cmp != 0 ? cmp : Long.compare(a.getId(), b.getId());
            });
            for (Exercise ex : pool) {
                if (result.size() >= need) break;
                result.add(ex);
            }
        }
        return result;
    }

    private int getGoalScore(Exercise ex, Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN -> ex.getMuscleGainScore()   != null ? ex.getMuscleGainScore()   : 0;
            case WEIGHT_LOSS -> ex.getWeightLossScore()   != null ? ex.getWeightLossScore()   : 0;
            case ENDURANCE   -> ex.getEnduranceScore()    != null ? ex.getEnduranceScore()    : 0;
            default          -> ex.getMaintenanceScore()  != null ? ex.getMaintenanceScore()  : 0;
        };
    }

    private int adjustDuration(int base, FitnessLevel lv) {
        return switch (lv) {
            case BEGINNER -> (int) (base * 0.7);
            case ADVANCED -> (int) (base * 1.3);
            default -> base;
        };
    }

    private int calcRest(Integer base, Goal goal) {
        if (base == null) base = 60;
        return switch (goal) {
            case MUSCLE_GAIN -> (int) (base * 1.3);
            case WEIGHT_LOSS -> (int) (base * 0.7);
            case ENDURANCE -> (int) (base * 0.6);
            default -> base;
        };
    }

    public List<List<Integer>> suggestDays(int sessions) {
        return ScheduleCatalog.candidatesFor(sessions);
    }

    private String buildScheduleNote(Goal goal, int sessions) {
        return switch (goal) {
            case MUSCLE_GAIN -> "💪 Tăng cơ cần nghỉ ít nhất 1 ngày giữa các buổi tập nhóm cơ giống nhau.";
            case WEIGHT_LOSS -> "🔥 Giảm cân hiệu quả khi tập liên tục.";
            case ENDURANCE -> "🏃 Sức bền cần xen kẽ ngày cardio và phục hồi.";
            default -> "⚖️ Duy trì đều đặn.";
        };
    }

    private String buildNote(Exercise ex, Goal goal) {
        int score = switch (goal) {
            case MUSCLE_GAIN -> ex.getMuscleGainScore() != null ? ex.getMuscleGainScore() : 0;
            case WEIGHT_LOSS -> ex.getWeightLossScore() != null ? ex.getWeightLossScore() : 0;
            case ENDURANCE -> ex.getEnduranceScore() != null ? ex.getEnduranceScore() : 0;
            default -> ex.getMaintenanceScore() != null ? ex.getMaintenanceScore() : 0;
        };
        if (score >= 9) return "⭐ Hàng đầu cho mục tiêu này";
        if (score >= 7) return "✅ Phù hợp tốt";
        return null;
    }

    // ─────────────────────────────────────────────────────────
    // Response Builders
    // ─────────────────────────────────────────────────────────
    public WorkoutPlanResponse toPlanResponse(WorkoutPlan plan, UserProfile profile) {
        List<WorkoutPlanDayResponse> days = Optional.ofNullable(plan.getPlanDays())
                .orElse(Collections.emptyList()).stream()
                .map(this::buildDayResponse).collect(Collectors.toList());

        List<List<Integer>> suggested = Boolean.TRUE.equals(plan.getIsAiGenerated())
                ? ScheduleCatalog.candidatesFor(plan.getSessionsPerWeek())
                : null;
        String note = buildScheduleNote(plan.getGoal(), plan.getSessionsPerWeek());

        // ── MỚI (Patch 7): ENDURANCE luôn đọc targetCurrentValue LIVE từ EnduranceTest,
        // KHÔNG dùng plan.getTargetCurrentValue() (cột này luôn null với ENDURANCE — LOCKED) ──
        Double liveTargetCurrentValue = plan.getTargetCurrentValue();
        if (plan.getGoal() == Goal.ENDURANCE && plan.getUser() != null && plan.getTargetMetricType() != null) {
            String metricType = plan.getTargetMetricType();
            liveTargetCurrentValue = enduranceTestRepo.findByUserId(plan.getUser().getId())
                    .map(test -> switch (metricType) {
                        case "PUSHUP_REPS" -> test.getPushupReps() != null ? test.getPushupReps().doubleValue() : null;
                        case "PLANK_SECONDS" -> test.getPlankSeconds() != null ? test.getPlankSeconds().doubleValue() : null;
                        case "SQUAT_REPS" -> test.getSquatReps() != null ? test.getSquatReps().doubleValue() : null;
                        default -> null;
                    })
                    .orElse(null);
        }

        return WorkoutPlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getPlanName())
                .description(plan.getDescription())
                .goal(plan.getGoal())
                .targetLevel(plan.getTargetLevel())
                .durationWeeks(plan.getDurationWeeks())
                .sessionsPerWeek(plan.getSessionsPerWeek())
                .currentWeek(plan.getCurrentWeek())
                .isActive(plan.getIsActive())
                .isAiGenerated(plan.getIsAiGenerated())
                .isTemplate(plan.getIsTemplate())
                .isCompleted(plan.getIsCompleted())
                .weekStartDate(plan.getWeekStartDate())
                .createdAt(plan.getCreatedAt())
                .startingBmi(plan.getStartingBmi())
                .startingWeight(plan.getStartingWeight())
                .difficultyAdjustment(plan.getDifficultyAdjustment())
                .setsAdjustment(plan.getSetsAdjustment())
                .repsAdjustment(plan.getRepsAdjustment())
                .planDays(days)
                .weightAdjustmentNote(plan.getWeightAdjustmentNote())
                .suggestedDays(suggested)
                .scheduleNote(Boolean.TRUE.equals(plan.getIsAiGenerated()) ? note : null)
                .maxMana(plan.getMaxMana())
                .currentMana(plan.getCurrentMana())
                .manaMessage(plan.getMaxMana() != null
                        ? ManaMessageHelper.buildMessage(plan.getMaxMana(), plan.getMaxMana())
                        : null)
                .fitnessScore(plan.getFitnessScore())
                .fitnessLevel(plan.getFitnessLevel() != null ? plan.getFitnessLevel().name() : null)
                .bodyType(plan.getBodyType() != null ? plan.getBodyType().name() : null)
                .targetMetricType(plan.getTargetMetricType())
                .targetBaselineValue(plan.getTargetBaselineValue())
                .targetGoalValue(plan.getTargetGoalValue())
                .targetCurrentValue(liveTargetCurrentValue)
                .targetAchieved(plan.getTargetAchieved())
                .estimatedWeeks(plan.getEstimatedWeeks())
                .build();
    }

    private WorkoutPlanDayResponse buildDayResponse(WorkoutPlanDay day) {
        return WorkoutPlanDayResponse.builder()
                .id(day.getId())
                .dayOfWeek(day.getDayOfWeek())
                .dayName(day.getDayName())
                .exercises(Optional.ofNullable(day.getExercises())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(this::buildExResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private WorkoutPlanExerciseResponse buildExResponse(WorkoutPlanExercise pe) {
        Exercise ex = pe.getExercise();
        boolean justRevealed = pe.getWeightUpdatedWeek() != null
                && pe.getPlanDay() != null
                && pe.getPlanDay().getWorkoutPlan() != null
                && pe.getWeightUpdatedWeek().equals(pe.getPlanDay().getWorkoutPlan().getCurrentWeek());

        return WorkoutPlanExerciseResponse.builder()
                .id(pe.getId())
                .exerciseId(ex.getId())
                .exerciseName(ex.getName())
                .muscleGroup(ex.getMuscleGroup() != null ? ex.getMuscleGroup().name() : null)
                .difficulty(ex.getDifficulty() != null ? ex.getDifficulty().name() : null)
                .sets(pe.getSets())
                .reps(pe.getReps())
                .durationSeconds(pe.getDurationSeconds())
                .restSeconds(pe.getRestSeconds())
                .orderIndex(pe.getOrderIndex())
                .notes(pe.getNotes())
                .videoUrl(ex.getVideoUrl())
                .caloriesBurned(ex.getCaloriesBurned())
                .baseWeightKg(pe.getBaseWeightKg())
                .currentWeightKg(pe.getCurrentWeightKg())
                .weightJustRevealed(justRevealed)
                .recommendedWeightKg(pe.getRecommendedWeightKg())
                .build();
    }

    private String buildPlanName(Goal goal, FitnessLevel lv) {
        String g = switch (goal) {
            case WEIGHT_LOSS -> "Fat Burning";
            case MUSCLE_GAIN -> "Muscle Building";
            case ENDURANCE -> "Endurance";
            default -> "Balanced";
        };
        String l = switch (lv) {
            case BEGINNER -> "Starter";
            case ADVANCED -> "Elite";
            default -> "Progress";
        };
        return g + " " + l + " Plan";
    }

    private String buildPlanDesc(Goal goal, FitnessLevel lv, int days, UserProfile profile) {
        String gv = switch (goal) {
            case WEIGHT_LOSS -> "giảm cân & đốt mỡ";
            case MUSCLE_GAIN -> "tăng cơ & sức mạnh";
            case ENDURANCE -> "tăng sức bền";
            default -> "duy trì thể hình";
        };
        String bmiNote = (profile != null && profile.getBmi() != null)
                ? " (BMI hiện tại: " + profile.getBmi() + ")" : "";
        return String.format("Giáo án AI cho mục tiêu %s%s. %d buổi/tuần. Cường độ điều chỉnh tự động theo tiến độ hàng tuần.",
                gv, bmiNote, days);
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void deactivateAndCleanOldPlan(Long userId) {
        planRepo.findByUserIdAndIsActiveTrue(userId).ifPresent(oldPlan -> {
            List<WorkoutSession> oldSessions = sessionRepo
                    .findByUserIdAndWorkoutPlanId(userId, oldPlan.getId());
            if (!oldSessions.isEmpty()) {
                sessionRepo.deleteAll(oldSessions);
            }
            oldPlan.setIsActive(false);
            planRepo.save(oldPlan);
        });
    }

    public WorkoutPlanResponse buildPlanResponse(WorkoutPlan plan) {
        return toPlanResponse(plan, null);
    }
}