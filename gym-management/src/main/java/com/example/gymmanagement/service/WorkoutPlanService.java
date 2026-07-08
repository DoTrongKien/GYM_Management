package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.TemplateDayRequest;
import com.example.gymmanagement.dto.request.TemplateExerciseRequest;
import com.example.gymmanagement.dto.request.WorkoutPlanRequest;
import com.example.gymmanagement.dto.request.WorkoutTemplateRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.*;
import com.example.gymmanagement.repository.*;
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
    // ── MỚI ──
    private final WorkoutPlanExerciseRepository planExerciseRepo;
    private final MembershipRepository membershipRepo;

    // Gói Free chỉ được tạo/đổi giáo án 1 lần/tháng. VIP không giới hạn.
    private static final int FREE_PLAN_LIMIT_PER_MONTH = 1;

    /** Chặn tạo/đổi giáo án nếu user gói Free đã dùng hết lượt trong tháng. VIP luôn được bỏ qua. */
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
    // 1. generateAIPlanWithGoal — FS + BodyType + Level + Goal + Mana
    // ─────────────────────────────────────────────────────────
    @Transactional
    public WorkoutPlanResponse generateAIPlanWithGoal(String email, Goal goal,
                                                      FitnessLevel levelParam, Integer daysPerWeek) {
        User user = getUser(email);
        checkPlanGenerationLimit(user);
        UserProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);

        FitnessLevel level = levelParam != null ? levelParam
                : (profile != null && profile.getFitnessLevel() != null
                   ? profile.getFitnessLevel() : FitnessLevel.BEGINNER);

        daysPerWeek = calcSessionsPerWeek(goal, daysPerWeek, profile);

        Double startBmi    = profile != null ? profile.getBmi()    : null;
        Double startWeight = profile != null ? profile.getWeight() : null;

        double fs = (profile != null && profile.getAge() != null
                && profile.getHeight() != null && profile.getWeight() != null)
                ? fitnessCalculator.calculateFS(profile.getAge(), profile.getHeight(), profile.getWeight())
                : 60.0;

        FitnessCalculator.FsLevel fsLevel = fitnessCalculator.getFsLevel(fs);

        FitnessCalculator.BodyType bodyType = (profile != null)
                ? fitnessCalculator.classifyBodyType(profile.getHeight(), profile.getWeight(), startBmi)
                : FitnessCalculator.BodyType.CAN_DOI;

        deactivateAndCleanOldPlan(user.getId());

        // ── MỚI: Quy đổi FS (0-100) sang Mana (0-200) ──
        int maxMana = (int) Math.round(fs * 2);

        WorkoutPlan plan = WorkoutPlan.builder()
                .user(user)
                .planName(buildPlanName(goal, level))
                .description(buildPlanDesc(goal, level, daysPerWeek, profile))
                .goal(goal).targetLevel(level)
                .durationWeeks(6).sessionsPerWeek(daysPerWeek).currentWeek(1)
                .startingBmi(startBmi).startingWeight(startWeight)
                .isActive(true).isAiGenerated(true)
                .maxMana(maxMana)
                .currentMana(maxMana)
                .build();
        planRepo.save(plan);

        List<WorkoutPlanDay> days = buildPlanDaysNew(plan, goal, level, fsLevel, bodyType, daysPerWeek);
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
    // 6. adjustPlanAfterWeek — đơn giản hóa (giữ nguyên logic cũ)
    // ─────────────────────────────────────────────────────────
    @Transactional
    public WorkoutPlanResponse adjustPlanAfterWeek(Long planId, String email,
                                                   Double newWeight, Double newBodyFat) {
        User user = getUser(email);
        WorkoutPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        int week = plan.getCurrentWeek() != null ? plan.getCurrentWeek() : 1;
        long completed = sessionRepo.countCompletedInWeek(user.getId(), planId, week);
        int  target    = plan.getSessionsPerWeek();

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
        if (completed < target) {
            plan.setDurationWeeks(plan.getDurationWeeks() + 1);
            note = "📅 Bạn bỏ " + (target - completed) + " buổi tuần này. Đã gia hạn thêm 1 tuần.";
        }

        plan.setCurrentWeek(week + 1);

        if (plan.getCurrentWeek() > plan.getDurationWeeks()) {
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

    // ─────────────────────────────────────────────────────────
    // MỚI: Nhập tạ khởi điểm (chỉ 1 lần, tuần đầu)
    // ─────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────
    // 4. ADMIN: Tạo template thủ công
    // ─────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────
    // 5. USER: Chọn 1 template -> copy thành giáo án active của user
    // ─────────────────────────────────────────────────────────
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
                // Template không dùng hệ thống mana AI -> để null, ManaService sẽ tự bỏ qua
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

    // ─────────────────────────────────────────────────────────
    // Helper cho template
    // ─────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────
    // Các hàm hỗ trợ — GIỮ NGUYÊN toàn bộ logic gốc
    // ─────────────────────────────────────────────────────────
    private int calcSessionsPerWeek(Goal goal, Integer requested, UserProfile profile) {
        int fromProfile = (profile != null && profile.getAvailableDaysPerWeek() != null)
                ? profile.getAvailableDaysPerWeek() : 3;
        int val = requested != null ? requested : fromProfile;
        val = Math.max(2, Math.min(6, val));

        int minRequired = switch (goal) {
            case MUSCLE_GAIN, WEIGHT_LOSS -> 4;
            case ENDURANCE -> 3;
            default -> 2;
        };
        return Math.max(val, minRequired);
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
                                                  int sessions) {
        List<List<MuscleGroup>> configs = getDayConfigs(goal, sessions);
        String[] names = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
        int[] dow = {1,3,5,2,4,6,7};

        int exPerGroup = calcExPerGroupByLevel(level, sessions);

        List<WorkoutPlanDay> days = new ArrayList<>();
        for (int i = 0; i < Math.min(sessions, configs.size()); i++) {
            WorkoutPlanDay day = WorkoutPlanDay.builder()
                    .workoutPlan(plan)
                    .dayOfWeek(dow[i])
                    .dayName(names[dow[i] - 1])
                    .build();
            day.setExercises(buildExercisesNew(day, configs.get(i), goal,
                    level, fsLevel, bodyType, exPerGroup));
            days.add(day);
        }
        return days;
    }

    private int calcExPerGroupByLevel(FitnessLevel level, int sessions) {
        int base = switch (sessions) {
            case 2 -> 3;
            case 3 -> 2;
            case 4 -> 2;
            default -> 1;
        };
        return switch (level) {
            case BEGINNER     -> Math.max(1, base - 1);
            case INTERMEDIATE -> base;
            case ADVANCED     -> base + 1;
        };
    }

    private List<WorkoutPlanExercise> buildExercisesNew(WorkoutPlanDay day,
                                                        List<MuscleGroup> groups,
                                                        Goal goal,
                                                        FitnessLevel level,
                                                        FitnessCalculator.FsLevel fsLevel,
                                                        FitnessCalculator.BodyType bodyType,
                                                        int exPerGroup) {
        int[] baseSR = fitnessCalculator.calcSetsRepsByFS(fsLevel, goal);

        int[] adj = fitnessCalculator.bodyTypeAdjustment(bodyType, goal);
        int finalSets = Math.max(1, baseSR[0] + adj[0]);
        int finalReps = Math.max(4, baseSR[1] + adj[1]);

        List<WorkoutPlanExercise> result = new ArrayList<>();
        int idx = 1;

        for (MuscleGroup mg : groups) {
            List<Exercise> cands = getExercisesByLevelAndGoal(mg, goal, level, exPerGroup);
            for (Exercise ex : cands) {
                result.add(WorkoutPlanExercise.builder()
                        .planDay(day).exercise(ex)
                        .sets(finalSets)
                        .reps(ex.getDefaultReps() != null ? finalReps : null)
                        .durationSeconds(ex.getDefaultDurationSeconds() != null
                                ? adjustDuration(ex.getDefaultDurationSeconds(), level) : null)
                        .restSeconds(calcRest(ex.getRestSeconds(), goal))
                        .orderIndex(idx++)
                        .notes(buildNote(ex, goal))
                        .build());
            }
        }
        return result;
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
            pool.sort((a, b) -> Integer.compare(
                    getGoalScore(b, goal), getGoalScore(a, goal)));
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
            case FLEXIBILITY -> ex.getFlexibilityScore()  != null ? ex.getFlexibilityScore()  : 0;
            default          -> ex.getMaintenanceScore()  != null ? ex.getMaintenanceScore()  : 0;
        };
    }

    private List<WorkoutPlanExercise> buildExercises(WorkoutPlanDay day,
                                                     List<MuscleGroup> groups, Goal goal, FitnessLevel level,
                                                     int exPerGroup, int setsAdj, int repsAdj) {
        List<WorkoutPlanExercise> result = new ArrayList<>();
        int idx = 1;
        for (MuscleGroup mg : groups) {
            List<Exercise> cands = getByGoalScore(mg, goal);
            int take = Math.min(cands.size(), exPerGroup);
            for (int i = 0; i < take; i++) {
                Exercise ex = cands.get(i);
                int[] sr = calcSetsReps(ex, goal, level, setsAdj, repsAdj);
                result.add(WorkoutPlanExercise.builder()
                        .planDay(day).exercise(ex)
                        .sets(sr[0]).reps(ex.getDefaultReps() != null ? sr[1] : null)
                        .durationSeconds(ex.getDefaultDurationSeconds() != null
                                ? adjustDuration(ex.getDefaultDurationSeconds(), level) : null)
                        .restSeconds(calcRest(ex.getRestSeconds(), goal))
                        .orderIndex(idx++).notes(buildNote(ex, goal)).build());
            }
        }
        return result;
    }

    private int[] calcSetsReps(Exercise ex, Goal goal, FitnessLevel lv, int setsAdj, int repsAdj) {
        int sets = switch (goal) {
            case MUSCLE_GAIN -> switch (lv) {
                case BEGINNER -> 3;
                case ADVANCED -> 5;
                default -> 4;
            };
            case WEIGHT_LOSS -> 3;
            case ENDURANCE   -> switch (lv) {
                case BEGINNER -> 2;
                default -> 3;
            };
            default -> ex.getDefaultSets() != null ? ex.getDefaultSets() : 3;
        };
        int reps = switch (goal) {
            case MUSCLE_GAIN -> switch (lv) {
                case BEGINNER -> 10;
                case ADVANCED -> 6;
                default -> 8;
            };
            case WEIGHT_LOSS -> switch (lv) {
                case BEGINNER -> 15;
                default -> 20;
            };
            case ENDURANCE -> 25;
            default -> ex.getDefaultReps() != null ? ex.getDefaultReps() : 12;
        };
        return new int[]{Math.max(1, sets + setsAdj), Math.max(4, reps + repsAdj)};
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

    public List<String> suggestDays(Goal goal, int sessions) {
        return switch (goal) {
            case MUSCLE_GAIN -> switch (sessions) {
                case 2 -> List.of("Monday", "Thursday");
                case 3 -> List.of("Monday", "Wednesday", "Friday");
                case 4 -> List.of("Monday", "Tuesday", "Thursday", "Friday");
                case 5 -> List.of("Monday", "Tuesday", "Wednesday", "Friday", "Saturday");
                default -> List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
            };
            case WEIGHT_LOSS -> switch (sessions) {
                case 2 -> List.of("Monday", "Wednesday");
                case 3 -> List.of("Monday", "Wednesday", "Friday");
                case 4 -> List.of("Monday", "Tuesday", "Thursday", "Friday");
                case 5 -> List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
                default -> List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
            };
            case ENDURANCE -> switch (sessions) {
                case 2 -> List.of("Tuesday", "Friday");
                case 3 -> List.of("Tuesday", "Thursday", "Saturday");
                case 4 -> List.of("Monday", "Wednesday", "Friday", "Sunday");
                default -> List.of("Monday", "Tuesday", "Thursday", "Friday", "Saturday");
            };
            default -> switch (sessions) {
                case 2 -> List.of("Monday", "Thursday");
                case 3 -> List.of("Monday", "Wednesday", "Friday");
                default -> List.of("Monday", "Wednesday", "Friday", "Sunday");
            };
        };
    }

    private String buildScheduleNote(Goal goal, int sessions) {
        return switch (goal) {
            case MUSCLE_GAIN -> "💪 Tăng cơ cần nghỉ ít nhất 1 ngày giữa các buổi tập nhóm cơ giống nhau.";
            case WEIGHT_LOSS -> "🔥 Giảm cân hiệu quả khi tập liên tục.";
            case ENDURANCE -> "🏃 Sức bền cần xen kẽ ngày cardio và phục hồi.";
            case FLEXIBILITY -> "🤸 Linh hoạt có thể tập mỗi ngày.";
            default -> "⚖️ Duy trì đều đặn.";
        };
    }

    private List<List<MuscleGroup>> getDayConfigs(Goal goal, int sessions) {
        List<List<MuscleGroup>> all = switch (goal) {
            case MUSCLE_GAIN -> List.of(
                    List.of(MuscleGroup.CHEST, MuscleGroup.ARMS),
                    List.of(MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                    List.of(MuscleGroup.LEGS),
                    List.of(MuscleGroup.CHEST, MuscleGroup.CORE),
                    List.of(MuscleGroup.BACK, MuscleGroup.ARMS),
                    List.of(MuscleGroup.LEGS, MuscleGroup.SHOULDERS)
            );
            case WEIGHT_LOSS -> List.of(
                    List.of(MuscleGroup.CARDIO, MuscleGroup.CORE),
                    List.of(MuscleGroup.FULL_BODY),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.LEGS),
                    List.of(MuscleGroup.BACK, MuscleGroup.CHEST),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.ARMS),
                    List.of(MuscleGroup.FULL_BODY, MuscleGroup.CORE)
            );
            case ENDURANCE -> List.of(
                    List.of(MuscleGroup.CARDIO),
                    List.of(MuscleGroup.FULL_BODY),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.CORE),
                    List.of(MuscleGroup.LEGS, MuscleGroup.CARDIO),
                    List.of(MuscleGroup.FULL_BODY, MuscleGroup.CORE),
                    List.of(MuscleGroup.CARDIO)
            );
            case FLEXIBILITY -> List.of(
                    List.of(MuscleGroup.FULL_BODY, MuscleGroup.CORE),
                    List.of(MuscleGroup.LEGS, MuscleGroup.BACK),
                    List.of(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                    List.of(MuscleGroup.FULL_BODY),
                    List.of(MuscleGroup.CORE, MuscleGroup.LEGS),
                    List.of(MuscleGroup.BACK, MuscleGroup.CHEST)
            );
            default -> List.of(
                    List.of(MuscleGroup.FULL_BODY),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.CORE),
                    List.of(MuscleGroup.CHEST, MuscleGroup.BACK),
                    List.of(MuscleGroup.LEGS),
                    List.of(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.FULL_BODY)
            );
        };
        return all.subList(0, Math.min(sessions, all.size()));
    }

    private List<Exercise> getByGoalScore(MuscleGroup mg, Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN -> exerciseRepo.findByMuscleGroupOrderByMuscleGain(mg);
            case WEIGHT_LOSS -> exerciseRepo.findByMuscleGroupOrderByWeightLoss(mg);
            case ENDURANCE -> exerciseRepo.findByMuscleGroupOrderByEndurance(mg);
            case FLEXIBILITY -> exerciseRepo.findByMuscleGroupOrderByFlexibility(mg);
            default -> exerciseRepo.findByMuscleGroupOrderByMaintenance(mg);
        };
    }

    private String buildNote(Exercise ex, Goal goal) {
        int score = switch (goal) {
            case MUSCLE_GAIN -> ex.getMuscleGainScore() != null ? ex.getMuscleGainScore() : 0;
            case WEIGHT_LOSS -> ex.getWeightLossScore() != null ? ex.getWeightLossScore() : 0;
            case ENDURANCE -> ex.getEnduranceScore() != null ? ex.getEnduranceScore() : 0;
            case FLEXIBILITY -> ex.getFlexibilityScore() != null ? ex.getFlexibilityScore() : 0;
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

        List<String> suggested = suggestDays(plan.getGoal(), plan.getSessionsPerWeek());
        String note = buildScheduleNote(plan.getGoal(), plan.getSessionsPerWeek());

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
                .suggestedDays(Boolean.TRUE.equals(plan.getIsAiGenerated()) ? suggested : null)
                .scheduleNote(Boolean.TRUE.equals(plan.getIsAiGenerated()) ? note : null)
                // ── MỚI: Mana ──
                .maxMana(plan.getMaxMana())
                .currentMana(plan.getCurrentMana())
                // Câu động viên dựa trên thể lực GỐC (maxMana, cố định theo FS lúc tạo giáo án),
                // không dùng currentMana vì con số đó bị trừ dần sau mỗi buổi tập.
                .manaMessage(plan.getMaxMana() != null
                        ? ManaMessageHelper.buildMessage(plan.getMaxMana(), plan.getMaxMana())
                        : null)
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
                .build();
    }

    private String buildPlanName(Goal goal, FitnessLevel lv) {
        String g = switch (goal) {
            case WEIGHT_LOSS -> "Fat Burning";
            case MUSCLE_GAIN -> "Muscle Building";
            case ENDURANCE -> "Endurance";
            case FLEXIBILITY -> "Flexibility";
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
            case FLEXIBILITY -> "tăng linh hoạt";
            default -> "duy trì thể hình";
        };
        String bmiNote = (profile != null && profile.getBmi() != null)
                ? " (BMI hiện tại: " + profile.getBmi() + ")" : "";
        return String.format("Giáo án AI 6 tuần cho mục tiêu %s%s. %d buổi/tuần. Cường độ điều chỉnh tự động theo tiến độ hàng tuần.",
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