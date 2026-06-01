package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.WorkoutPlanRequest;
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

    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanDayRepository planDayRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserProfileRepository profileRepository;

    // ── Tạo giáo án AI ──────────────────────────────────────────
    @Transactional
    public WorkoutPlanResponse generateAIPlan(String email) {
        User user = getUser(email);

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Hãy hoàn thiện hồ sơ trước khi tạo giáo án."));

        Goal goal = profile.getGoal() != null ? profile.getGoal() : Goal.MAINTENANCE;
        FitnessLevel level = profile.getFitnessLevel() != null ? profile.getFitnessLevel() : FitnessLevel.BEGINNER;
        int daysPerWeek = profile.getAvailableDaysPerWeek() != null ? profile.getAvailableDaysPerWeek() : 3;

        return buildAndSavePlan(user, goal, level, daysPerWeek, true);
    }

    // ── Tạo giáo án AI với mục tiêu tuỳ chọn ───────────────────
    @Transactional
    public WorkoutPlanResponse generateAIPlanWithGoal(String email, Goal goal, FitnessLevel level, Integer daysPerWeek) {
        User user = getUser(email);

        // Lấy daysPerWeek từ profile nếu không truyền
        if (daysPerWeek == null || daysPerWeek < 1) {
            daysPerWeek = profileRepository.findByUserId(user.getId())
                    .map(p -> p.getAvailableDaysPerWeek() != null ? p.getAvailableDaysPerWeek() : 3)
                    .orElse(3);
        }
        if (level == null) {
            level = profileRepository.findByUserId(user.getId())
                    .map(p -> p.getFitnessLevel() != null ? p.getFitnessLevel() : FitnessLevel.BEGINNER)
                    .orElse(FitnessLevel.BEGINNER);
        }

        return buildAndSavePlan(user, goal, level, daysPerWeek, true);
    }

    private WorkoutPlanResponse buildAndSavePlan(User user, Goal goal, FitnessLevel level, int daysPerWeek, boolean isAi) {
        // Deactivate plan cũ
        planRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(p -> {
            p.setIsActive(false);
            planRepository.save(p);
        });

        WorkoutPlan plan = WorkoutPlan.builder()
                .user(user)
                .planName(generatePlanName(goal, level))
                .description(generatePlanDescription(goal, level, daysPerWeek))
                .goal(goal)
                .targetLevel(level)
                .durationWeeks(8)
                .sessionsPerWeek(daysPerWeek)
                .isActive(true)
                .isAiGenerated(isAi)
                .build();
        planRepository.save(plan);

        List<WorkoutPlanDay> days = generatePlanDays(plan, goal, level, daysPerWeek);
        planDayRepository.saveAll(days);

        generateWeeklySessions(user, plan, days, 8);

        plan.setPlanDays(days);
        return buildPlanResponse(plan);
    }

    // ── Tạo giáo án thủ công ────────────────────────────────────
    @Transactional
    public WorkoutPlanResponse createCustomPlan(String email, WorkoutPlanRequest request) {
        User user = getUser(email);

        planRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(p -> {
            p.setIsActive(false);
            planRepository.save(p);
        });

        WorkoutPlan plan = WorkoutPlan.builder()
                .user(user)
                .planName(request.getPlanName())
                .description(request.getDescription())
                .goal(request.getGoal())
                .targetLevel(request.getTargetLevel())
                .durationWeeks(request.getDurationWeeks() != null ? request.getDurationWeeks() : 4)
                .sessionsPerWeek(request.getSessionsPerWeek() != null ? request.getSessionsPerWeek() : 3)
                .isActive(true)
                .isAiGenerated(false)
                .build();
        planRepository.save(plan);

        return buildPlanResponse(plan);
    }

    public WorkoutPlanResponse getActivePlan(String email) {
        User user = getUser(email);
        WorkoutPlan plan = planRepository.findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new RuntimeException("Chưa có giáo án active. Hãy tạo giáo án mới."));
        plan.setPlanDays(planDayRepository.findByWorkoutPlanIdOrderByDayOfWeek(plan.getId()));
        return buildPlanResponse(plan);
    }

    public List<WorkoutPlanResponse> getAllPlans(String email) {
        User user = getUser(email);
        return planRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(p -> {
                    p.setPlanDays(planDayRepository.findByWorkoutPlanIdOrderByDayOfWeek(p.getId()));
                    return buildPlanResponse(p);
                })
                .collect(Collectors.toList());
    }

    // ── Core: tạo các ngày tập dựa theo goal ────────────────────
    private List<WorkoutPlanDay> generatePlanDays(WorkoutPlan plan, Goal goal, FitnessLevel level, int daysPerWeek) {
        // Cấu hình nhóm cơ theo mục tiêu
        List<List<MuscleGroup>> dayConfigs = getDayConfigs(goal, daysPerWeek);

        int[] dayOfWeekMap = {1, 3, 5, 2, 4, 6, 7};
        String[] dayNames  = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};

        List<WorkoutPlanDay> days = new ArrayList<>();

        for (int i = 0; i < Math.min(daysPerWeek, dayConfigs.size()); i++) {
            WorkoutPlanDay day = WorkoutPlanDay.builder()
                    .workoutPlan(plan)
                    .dayOfWeek(dayOfWeekMap[i])
                    .dayName(dayNames[dayOfWeekMap[i] - 1])
                    .build();

            List<WorkoutPlanExercise> exercises = generateExercisesForDay(day, dayConfigs.get(i), goal, level);
            day.setExercises(exercises);
            days.add(day);
        }
        return days;
    }

    /**
     * Cấu hình nhóm cơ cho từng ngày tập theo mục tiêu.
     * MUSCLE_GAIN  → tập cơ nặng, chia bộ phận rõ ràng
     * WEIGHT_LOSS  → nhiều cardio + toàn thân
     * ENDURANCE    → cardio + cơ lõi + toàn thân
     * FLEXIBILITY  → linh hoạt, yoga, kéo giãn
     * MAINTENANCE  → cân bằng tất cả
     */
    private List<List<MuscleGroup>> getDayConfigs(Goal goal, int days) {
        List<List<MuscleGroup>> all;

        switch (goal) {
            case MUSCLE_GAIN:
                all = List.of(
                        List.of(MuscleGroup.CHEST, MuscleGroup.ARMS),
                        List.of(MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                        List.of(MuscleGroup.LEGS),
                        List.of(MuscleGroup.CHEST, MuscleGroup.CORE),
                        List.of(MuscleGroup.BACK, MuscleGroup.ARMS),
                        List.of(MuscleGroup.LEGS, MuscleGroup.SHOULDERS),
                        List.of(MuscleGroup.FULL_BODY)
                );
                break;
            case WEIGHT_LOSS:
                all = List.of(
                        List.of(MuscleGroup.CARDIO, MuscleGroup.CORE),
                        List.of(MuscleGroup.FULL_BODY),
                        List.of(MuscleGroup.CARDIO, MuscleGroup.LEGS),
                        List.of(MuscleGroup.BACK, MuscleGroup.CHEST),
                        List.of(MuscleGroup.CARDIO, MuscleGroup.ARMS),
                        List.of(MuscleGroup.FULL_BODY, MuscleGroup.CORE),
                        List.of(MuscleGroup.CARDIO)
                );
                break;
            case ENDURANCE:
                all = List.of(
                        List.of(MuscleGroup.CARDIO),
                        List.of(MuscleGroup.FULL_BODY),
                        List.of(MuscleGroup.CARDIO, MuscleGroup.CORE),
                        List.of(MuscleGroup.LEGS, MuscleGroup.CARDIO),
                        List.of(MuscleGroup.FULL_BODY, MuscleGroup.CORE),
                        List.of(MuscleGroup.CARDIO),
                        List.of(MuscleGroup.FULL_BODY)
                );
                break;
            case FLEXIBILITY:
                all = List.of(
                        List.of(MuscleGroup.FULL_BODY, MuscleGroup.CORE),
                        List.of(MuscleGroup.LEGS, MuscleGroup.BACK),
                        List.of(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                        List.of(MuscleGroup.FULL_BODY),
                        List.of(MuscleGroup.CORE, MuscleGroup.LEGS),
                        List.of(MuscleGroup.BACK, MuscleGroup.CHEST),
                        List.of(MuscleGroup.FULL_BODY)
                );
                break;
            default: // MAINTENANCE
                all = List.of(
                        List.of(MuscleGroup.FULL_BODY),
                        List.of(MuscleGroup.CARDIO, MuscleGroup.CORE),
                        List.of(MuscleGroup.CHEST, MuscleGroup.BACK),
                        List.of(MuscleGroup.LEGS),
                        List.of(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                        List.of(MuscleGroup.CARDIO, MuscleGroup.FULL_BODY),
                        List.of(MuscleGroup.CORE)
                );
        }

        return all.subList(0, Math.min(days, all.size()));
    }

    /**
     * Chọn bài tập cho 1 ngày dựa theo mục tiêu.
     * Ưu tiên bài tập có score cao nhất cho goal đó.
     * Số bài tập tùy trình độ: Beginner=2, Inter=3, Advanced=4
     */
    private List<WorkoutPlanExercise> generateExercisesForDay(
            WorkoutPlanDay day, List<MuscleGroup> muscleGroups, Goal goal, FitnessLevel level) {

        List<WorkoutPlanExercise> result = new ArrayList<>();
        int exercisesPerGroup = level == FitnessLevel.BEGINNER ? 2 : level == FitnessLevel.INTERMEDIATE ? 3 : 4;
        int orderIndex = 1;

        for (MuscleGroup mg : muscleGroups) {
            // Lấy danh sách bài tập của nhóm cơ này, ưu tiên theo score mục tiêu
            List<Exercise> candidates = getExercisesByGoalScore(mg, goal);

            int take = Math.min(candidates.size(), exercisesPerGroup);
            for (int i = 0; i < take; i++) {
                Exercise ex = candidates.get(i);

                // Điều chỉnh sets/reps theo trình độ và mục tiêu
                int[] setsReps = adjustSetsReps(ex, goal, level);

                WorkoutPlanExercise planEx = WorkoutPlanExercise.builder()
                        .planDay(day)
                        .exercise(ex)
                        .sets(setsReps[0])
                        .reps(ex.getDefaultReps() != null ? setsReps[1] : null)
                        .durationSeconds(ex.getDefaultDurationSeconds() != null
                                ? adjustDuration(ex.getDefaultDurationSeconds(), level) : null)
                        .restSeconds(adjustRest(ex.getRestSeconds(), goal))
                        .orderIndex(orderIndex++)
                        .notes(buildNotes(ex, goal))
                        .build();
                result.add(planEx);
            }
        }
        return result;
    }

    /**
     * Lấy danh sách bài tập của nhóm cơ, sắp xếp theo score mục tiêu giảm dần.
     */
    private List<Exercise> getExercisesByGoalScore(MuscleGroup mg, Goal goal) {
        switch (goal) {
            case MUSCLE_GAIN:  return exerciseRepository.findByMuscleGroupOrderByMuscleGain(mg);
            case WEIGHT_LOSS:  return exerciseRepository.findByMuscleGroupOrderByWeightLoss(mg);
            case ENDURANCE:    return exerciseRepository.findByMuscleGroupOrderByEndurance(mg);
            case FLEXIBILITY:  return exerciseRepository.findByMuscleGroupOrderByFlexibility(mg);
            default:           return exerciseRepository.findByMuscleGroupOrderByMaintenance(mg);
        }
    }

    /**
     * Điều chỉnh sets/reps theo mục tiêu:
     * MUSCLE_GAIN  → ít reps, nhiều tạ (4x6-10)
     * WEIGHT_LOSS  → nhiều reps, ít nghỉ (3x15-20)
     * ENDURANCE    → nhiều reps siêu nhẹ (3x20+)
     * FLEXIBILITY  → ít sets, giữ lâu
     * MAINTENANCE  → cân bằng (3x12)
     */
    private int[] adjustSetsReps(Exercise ex, Goal goal, FitnessLevel level) {
        int baseSets = ex.getDefaultSets() != null ? ex.getDefaultSets() : 3;
        int baseReps = ex.getDefaultReps() != null ? ex.getDefaultReps() : 12;

        int sets, reps;
        switch (goal) {
            case MUSCLE_GAIN:
                sets = level == FitnessLevel.BEGINNER ? 3 : level == FitnessLevel.INTERMEDIATE ? 4 : 5;
                reps = level == FitnessLevel.BEGINNER ? 10 : level == FitnessLevel.INTERMEDIATE ? 8 : 6;
                break;
            case WEIGHT_LOSS:
                sets = 3;
                reps = level == FitnessLevel.BEGINNER ? 15 : 20;
                break;
            case ENDURANCE:
                sets = level == FitnessLevel.BEGINNER ? 2 : 3;
                reps = level == FitnessLevel.BEGINNER ? 15 : 25;
                break;
            case FLEXIBILITY:
                sets = 2;
                reps = baseReps; // sẽ dùng duration thay reps
                break;
            default: // MAINTENANCE
                sets = baseSets;
                reps = baseReps;
        }
        return new int[]{sets, reps};
    }

    private int adjustDuration(int baseDuration, FitnessLevel level) {
        return switch (level) {
            case BEGINNER     -> (int)(baseDuration * 0.7);
            case INTERMEDIATE -> baseDuration;
            case ADVANCED     -> (int)(baseDuration * 1.3);
        };
    }

    private int adjustRest(Integer baseRest, Goal goal) {
        if (baseRest == null) baseRest = 60;
        return switch (goal) {
            case MUSCLE_GAIN  -> (int)(baseRest * 1.3); // nghỉ nhiều hơn để tạ nặng hơn
            case WEIGHT_LOSS  -> (int)(baseRest * 0.7); // nghỉ ít để nhịp tim cao
            case ENDURANCE    -> (int)(baseRest * 0.6);
            case FLEXIBILITY  -> 30;
            default           -> baseRest;
        };
    }

    private String buildNotes(Exercise ex, Goal goal) {
        int score = getGoalScore(ex, goal);
        if (score >= 9) return "⭐ Bài tập hàng đầu cho mục tiêu này";
        if (score >= 7) return "✅ Phù hợp tốt với mục tiêu";
        return null;
    }

    private int getGoalScore(Exercise ex, Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN  -> ex.getMuscleGainScore()  != null ? ex.getMuscleGainScore()  : 0;
            case WEIGHT_LOSS  -> ex.getWeightLossScore()  != null ? ex.getWeightLossScore()  : 0;
            case ENDURANCE    -> ex.getEnduranceScore()   != null ? ex.getEnduranceScore()   : 0;
            case FLEXIBILITY  -> ex.getFlexibilityScore() != null ? ex.getFlexibilityScore() : 0;
            default           -> ex.getMaintenanceScore() != null ? ex.getMaintenanceScore() : 0;
        };
    }

    private void generateWeeklySessions(User user, WorkoutPlan plan, List<WorkoutPlanDay> days, int weeks) {
        List<com.example.gymmanagement.entity.WorkoutSession> sessions = new ArrayList<>();
        LocalDate startDate = LocalDate.now();

        for (int week = 1; week <= weeks; week++) {
            for (WorkoutPlanDay day : days) {
                LocalDate sessionDate = startDate.plusWeeks(week - 1)
                        .with(java.time.DayOfWeek.of(day.getDayOfWeek()));
                sessions.add(com.example.gymmanagement.entity.WorkoutSession.builder()
                        .user(user).workoutPlan(plan).planDay(day)
                        .sessionDate(sessionDate)
                        .status(com.example.gymmanagement.enums.SessionStatus.SCHEDULED)
                        .weekNumber(week)
                        .isCustom(false)
                        .build());
            }
        }
        sessionRepository.saveAll(sessions);
    }

    private String generatePlanName(Goal goal, FitnessLevel level) {
        String g = switch (goal) {
            case WEIGHT_LOSS  -> "Fat Burning";
            case MUSCLE_GAIN  -> "Muscle Building";
            case ENDURANCE    -> "Endurance";
            case FLEXIBILITY  -> "Flexibility";
            default           -> "Balanced";
        };
        String l = switch (level) {
            case BEGINNER     -> "Starter";
            case INTERMEDIATE -> "Progress";
            case ADVANCED     -> "Elite";
        };
        return g + " " + l + " Plan";
    }

    private String generatePlanDescription(Goal goal, FitnessLevel level, int days) {
        String goalVi = switch (goal) {
            case WEIGHT_LOSS  -> "giảm cân & đốt mỡ";
            case MUSCLE_GAIN  -> "tăng cơ & sức mạnh";
            case ENDURANCE    -> "tăng sức bền";
            case FLEXIBILITY  -> "tăng linh hoạt";
            default           -> "duy trì thể hình";
        };
        return String.format(
                "Giáo án AI tối ưu cho mục tiêu %s, trình độ %s. %d buổi/tuần, 8 tuần. " +
                        "Bài tập được chọn lọc theo chỉ số phù hợp mục tiêu của bạn.",
                goalVi, level.name().toLowerCase(), days
        );
    }

    // ── Build response ───────────────────────────────────────────
    public WorkoutPlanResponse buildPlanResponse(WorkoutPlan plan) {
        List<WorkoutPlanDayResponse> dayResponses = Optional.ofNullable(plan.getPlanDays())
                .orElse(Collections.emptyList()).stream()
                .map(this::buildDayResponse)
                .collect(Collectors.toList());

        return WorkoutPlanResponse.builder()
                .id(plan.getId()).planName(plan.getPlanName())
                .description(plan.getDescription()).goal(plan.getGoal())
                .targetLevel(plan.getTargetLevel()).durationWeeks(plan.getDurationWeeks())
                .sessionsPerWeek(plan.getSessionsPerWeek()).isActive(plan.getIsActive())
                .isAiGenerated(plan.getIsAiGenerated()).createdAt(plan.getCreatedAt())
                .planDays(dayResponses).build();
    }

    private WorkoutPlanDayResponse buildDayResponse(WorkoutPlanDay day) {
        return WorkoutPlanDayResponse.builder()
                .id(day.getId()).dayOfWeek(day.getDayOfWeek()).dayName(day.getDayName())
                .exercises(Optional.ofNullable(day.getExercises()).orElse(Collections.emptyList())
                        .stream().map(this::buildExerciseResponse).collect(Collectors.toList()))
                .build();
    }

    private WorkoutPlanExerciseResponse buildExerciseResponse(WorkoutPlanExercise pe) {
        Exercise ex = pe.getExercise();
        return WorkoutPlanExerciseResponse.builder()
                .id(pe.getId()).exerciseId(ex.getId()).exerciseName(ex.getName())
                .muscleGroup(ex.getMuscleGroup() != null ? ex.getMuscleGroup().name() : null)
                .difficulty(ex.getDifficulty() != null ? ex.getDifficulty().name() : null)
                .sets(pe.getSets()).reps(pe.getReps()).durationSeconds(pe.getDurationSeconds())
                .restSeconds(pe.getRestSeconds()).orderIndex(pe.getOrderIndex())
                .notes(pe.getNotes()).videoUrl(ex.getVideoUrl()).caloriesBurned(ex.getCaloriesBurned())
                .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}