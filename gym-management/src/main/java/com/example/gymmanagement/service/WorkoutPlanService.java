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

    // ── Generate AI Plan ────────────────────────────────────────
    @Transactional
    public WorkoutPlanResponse generateAIPlan(String email) {
        User user = getUser(email);
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Hãy hoàn thiện hồ sơ trước khi tạo giáo án."));

        Goal goal         = profile.getGoal()         != null ? profile.getGoal()         : Goal.MAINTENANCE;
        FitnessLevel level= profile.getFitnessLevel() != null ? profile.getFitnessLevel() : FitnessLevel.BEGINNER;
        int daysPerWeek   = profile.getAvailableDaysPerWeek() != null ? profile.getAvailableDaysPerWeek() : 3;

        return buildAndSavePlan(user, goal, level, daysPerWeek, true);
    }

    // ── Generate Plan với mục tiêu tùy chọn ────────────────────
    @Transactional
    public WorkoutPlanResponse generatePlanWithGoal(String email, Goal goal, FitnessLevel level, Integer daysPerWeek) {
        User user = getUser(email);

        // Nếu không truyền thì lấy từ profile
        if (goal == null || level == null || daysPerWeek == null) {
            UserProfile profile = profileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Hãy hoàn thiện hồ sơ trước."));
            if (goal        == null) goal        = profile.getGoal()         != null ? profile.getGoal()         : Goal.MAINTENANCE;
            if (level       == null) level       = profile.getFitnessLevel() != null ? profile.getFitnessLevel() : FitnessLevel.BEGINNER;
            if (daysPerWeek == null) daysPerWeek = profile.getAvailableDaysPerWeek() != null ? profile.getAvailableDaysPerWeek() : 3;
        }

        return buildAndSavePlan(user, goal, level, daysPerWeek, true);
    }

    private WorkoutPlanResponse buildAndSavePlan(User user, Goal goal, FitnessLevel level, int daysPerWeek, boolean isAi) {
        // Deactivate old plan
        planRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(p -> {
            p.setIsActive(false);
            planRepository.save(p);
        });

        WorkoutPlan plan = WorkoutPlan.builder()
                .user(user)
                .planName(generatePlanName(goal, level))
                .description(generateDescription(goal, level, daysPerWeek))
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

    // ── Tạo plan thủ công ───────────────────────────────────────
    @Transactional
    public WorkoutPlanResponse createCustomPlan(String email, WorkoutPlanRequest request) {
        User user = getUser(email);
        planRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(p -> {
            p.setIsActive(false); planRepository.save(p);
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
                .orElseThrow(() -> new RuntimeException("Chưa có giáo án active."));
        plan.setPlanDays(planDayRepository.findByWorkoutPlanIdOrderByDayOfWeek(plan.getId()));
        return buildPlanResponse(plan);
    }

    public List<WorkoutPlanResponse> getAllPlans(String email) {
        User user = getUser(email);
        return planRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(p -> { p.setPlanDays(planDayRepository.findByWorkoutPlanIdOrderByDayOfWeek(p.getId())); return buildPlanResponse(p); })
                .collect(Collectors.toList());
    }

    // ── Core: tạo các ngày trong tuần ──────────────────────────
    private List<WorkoutPlanDay> generatePlanDays(WorkoutPlan plan, Goal goal, FitnessLevel level, int daysPerWeek) {
        List<String[]> schedule = getSchedule(goal, daysPerWeek);
        int[] dayOfWeekMap = {1, 3, 5, 2, 4, 6, 7};
        String[] dayNames  = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};

        List<WorkoutPlanDay> days = new ArrayList<>();
        for (int i = 0; i < Math.min(daysPerWeek, schedule.size()); i++) {
            WorkoutPlanDay day = WorkoutPlanDay.builder()
                    .workoutPlan(plan)
                    .dayOfWeek(dayOfWeekMap[i])
                    .dayName(dayNames[dayOfWeekMap[i] - 1])
                    .build();

            List<WorkoutPlanExercise> exercises = buildExercisesForDay(day, schedule.get(i), goal, level);
            day.setExercises(exercises);
            days.add(day);
        }
        return days;
    }

    // ── Lịch tập theo mục tiêu (nhóm cơ mỗi ngày) ─────────────
    private List<String[]> getSchedule(Goal goal, int days) {
        List<String[]> full = switch (goal) {
            case MUSCLE_GAIN -> List.of(
                    new String[]{"CHEST","ARMS"},
                    new String[]{"BACK","SHOULDERS"},
                    new String[]{"LEGS"},
                    new String[]{"CHEST","CORE"},
                    new String[]{"BACK","ARMS"},
                    new String[]{"LEGS","SHOULDERS"},
                    new String[]{"FULL_BODY"}
            );
            case WEIGHT_LOSS -> List.of(
                    new String[]{"CARDIO","CORE"},
                    new String[]{"FULL_BODY"},
                    new String[]{"CARDIO","LEGS"},
                    new String[]{"BACK","CHEST"},
                    new String[]{"CARDIO","ARMS"},
                    new String[]{"FULL_BODY","CORE"},
                    new String[]{"CARDIO"}
            );
            case ENDURANCE -> List.of(
                    new String[]{"CARDIO"},
                    new String[]{"LEGS","CORE"},
                    new String[]{"CARDIO"},
                    new String[]{"FULL_BODY"},
                    new String[]{"CARDIO","SHOULDERS"},
                    new String[]{"LEGS"},
                    new String[]{"CARDIO","CORE"}
            );
            case FLEXIBILITY -> List.of(
                    new String[]{"CORE","SHOULDERS"},
                    new String[]{"LEGS","CORE"},
                    new String[]{"BACK","SHOULDERS"},
                    new String[]{"CORE","LEGS"},
                    new String[]{"FULL_BODY"},
                    new String[]{"CORE"},
                    new String[]{"LEGS","SHOULDERS"}
            );
            default -> List.of(  // MAINTENANCE
                    new String[]{"FULL_BODY"},
                    new String[]{"CARDIO","CORE"},
                    new String[]{"CHEST","BACK"},
                    new String[]{"LEGS"},
                    new String[]{"SHOULDERS","ARMS"},
                    new String[]{"CARDIO"},
                    new String[]{"CORE","FULL_BODY"}
            );
        };
        return full.subList(0, Math.min(days, full.size()));
    }

    // ── Chọn bài tập theo SCORE cao nhất cho mục tiêu ──────────
    private List<WorkoutPlanExercise> buildExercisesForDay(WorkoutPlanDay day, String[] muscleGroups, Goal goal, FitnessLevel level) {
        List<WorkoutPlanExercise> result = new ArrayList<>();
        int orderIndex = 1;
        int maxPerGroup = level == FitnessLevel.BEGINNER ? 2 : level == FitnessLevel.INTERMEDIATE ? 3 : 4;

        for (String groupStr : muscleGroups) {
            try {
                MuscleGroup mg = MuscleGroup.valueOf(groupStr.trim());

                // Lấy bài tập sắp xếp theo score của mục tiêu (cao → thấp)
                List<Exercise> pool = getExercisesByGoalScore(mg, goal);

                // Lọc theo difficulty phù hợp với level
                List<Exercise> filtered = filterByLevel(pool, level);
                if (filtered.isEmpty()) filtered = pool; // fallback nếu không có

                int count = Math.min(filtered.size(), maxPerGroup);
                for (int i = 0; i < count; i++) {
                    Exercise ex = filtered.get(i);
                    result.add(WorkoutPlanExercise.builder()
                            .planDay(day)
                            .exercise(ex)
                            .sets(adjustSets(ex.getDefaultSets(), level, goal))
                            .reps(ex.getDefaultReps())
                            .durationSeconds(ex.getDefaultDurationSeconds())
                            .restSeconds(adjustRest(ex.getRestSeconds(), goal))
                            .orderIndex(orderIndex++)
                            .notes(buildNotes(ex, goal))
                            .build());
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    // ── Lấy bài tập theo score mục tiêu ─────────────────────────
    private List<Exercise> getExercisesByGoalScore(MuscleGroup mg, Goal goal) {
        List<Exercise> all = exerciseRepository.findByMuscleGroupAndIsActiveTrue(mg);
        return all.stream()
                .sorted((a, b) -> getScore(b, goal) - getScore(a, goal))
                .collect(Collectors.toList());
    }

    private int getScore(Exercise ex, Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN  -> ex.getMuscleGainScore()  != null ? ex.getMuscleGainScore()  : 5;
            case WEIGHT_LOSS  -> ex.getWeightLossScore()  != null ? ex.getWeightLossScore()  : 5;
            case ENDURANCE    -> ex.getEnduranceScore()   != null ? ex.getEnduranceScore()   : 5;
            case FLEXIBILITY  -> ex.getFlexibilityScore() != null ? ex.getFlexibilityScore() : 5;
            default           -> ex.getMaintenanceScore() != null ? ex.getMaintenanceScore() : 5;
        };
    }

    private List<Exercise> filterByLevel(List<Exercise> pool, FitnessLevel level) {
        Difficulty target = switch (level) {
            case BEGINNER     -> Difficulty.EASY;
            case INTERMEDIATE -> Difficulty.MEDIUM;
            case ADVANCED     -> Difficulty.HARD;
        };
        // BEGINNER → EASY, INTERMEDIATE → EASY+MEDIUM, ADVANCED → tất cả
        return pool.stream().filter(e -> switch (level) {
            case BEGINNER     -> e.getDifficulty() == Difficulty.EASY;
            case INTERMEDIATE -> e.getDifficulty() != Difficulty.HARD;
            case ADVANCED     -> true;
        }).collect(Collectors.toList());
    }

    // Điều chỉnh sets theo level và mục tiêu
    private Integer adjustSets(Integer base, FitnessLevel level, Goal goal) {
        if (base == null) return 3;
        int adjusted = base;
        if (level == FitnessLevel.BEGINNER)  adjusted = Math.max(2, base - 1);
        if (level == FitnessLevel.ADVANCED)  adjusted = base + 1;
        if (goal == Goal.WEIGHT_LOSS)        adjusted = Math.max(adjusted, 3); // nhiều sets giảm cân
        return adjusted;
    }

    // Điều chỉnh thời gian nghỉ theo mục tiêu
    private Integer adjustRest(Integer base, Goal goal) {
        if (base == null) return 60;
        return switch (goal) {
            case MUSCLE_GAIN -> base + 30;    // nghỉ dài hơn để tăng cơ
            case WEIGHT_LOSS -> Math.max(30, base - 15); // nghỉ ngắn để giữ nhịp tim
            case ENDURANCE   -> Math.max(20, base - 20); // nghỉ ngắn nhất
            default          -> base;
        };
    }

    private String buildNotes(Exercise ex, Goal goal) {
        int score = getScore(ex, goal);
        if (score >= 9) return "⭐ Bài tập tối ưu cho mục tiêu của bạn";
        if (score >= 7) return "✅ Phù hợp tốt với mục tiêu";
        return null;
    }

    // ── Sessions generator ───────────────────────────────────────
    private void generateWeeklySessions(User user, WorkoutPlan plan, List<WorkoutPlanDay> days, int weeks) {
        List<WorkoutSession> sessions = new ArrayList<>();
        LocalDate startDate = LocalDate.now();
        for (int week = 1; week <= weeks; week++) {
            for (WorkoutPlanDay day : days) {
                LocalDate sessionDate = startDate.plusWeeks(week - 1)
                        .with(java.time.DayOfWeek.of(day.getDayOfWeek()));
                sessions.add(WorkoutSession.builder()
                        .user(user).workoutPlan(plan).planDay(day)
                        .sessionDate(sessionDate)
                        .status(SessionStatus.SCHEDULED)
                        .weekNumber(week).build());
            }
        }
        sessionRepository.saveAll(sessions);
    }

    // ── Tên và mô tả giáo án ─────────────────────────────────────
    private String generatePlanName(Goal goal, FitnessLevel level) {
        String g = switch (goal) {
            case WEIGHT_LOSS -> "Fat Burning";
            case MUSCLE_GAIN -> "Muscle Builder";
            case ENDURANCE   -> "Endurance Pro";
            case FLEXIBILITY -> "Flexibility Flow";
            default          -> "Balanced Fitness";
        };
        String l = switch (level) {
            case BEGINNER     -> "Starter";
            case INTERMEDIATE -> "Progress";
            case ADVANCED     -> "Elite";
        };
        return g + " — " + l;
    }

    private String generateDescription(Goal goal, FitnessLevel level, int days) {
        return String.format(
                "Giáo án AI %d ngày/tuần trong 8 tuần, dành cho cấp độ %s. " +
                        "Bài tập được chọn lọc theo điểm hiệu quả cao nhất cho mục tiêu %s. " +
                        "Sets, reps và thời gian nghỉ được tối ưu theo cấp độ của bạn.",
                days,
                switch (level) { case BEGINNER -> "Mới bắt đầu"; case INTERMEDIATE -> "Trung bình"; default -> "Nâng cao"; },
                switch (goal) { case WEIGHT_LOSS -> "Giảm cân"; case MUSCLE_GAIN -> "Tăng cơ"; case ENDURANCE -> "Sức bền"; case FLEXIBILITY -> "Linh hoạt"; default -> "Duy trì"; }
        );
    }

    // ── Build responses ──────────────────────────────────────────
    public WorkoutPlanResponse buildPlanResponse(WorkoutPlan plan) {
        List<WorkoutPlanDayResponse> dayRes = Optional.ofNullable(plan.getPlanDays())
                .orElse(Collections.emptyList()).stream()
                .map(this::buildDayResponse).collect(Collectors.toList());
        return WorkoutPlanResponse.builder()
                .id(plan.getId()).planName(plan.getPlanName()).description(plan.getDescription())
                .goal(plan.getGoal()).targetLevel(plan.getTargetLevel())
                .durationWeeks(plan.getDurationWeeks()).sessionsPerWeek(plan.getSessionsPerWeek())
                .isActive(plan.getIsActive()).isAiGenerated(plan.getIsAiGenerated())
                .createdAt(plan.getCreatedAt()).planDays(dayRes).build();
    }

    private WorkoutPlanDayResponse buildDayResponse(WorkoutPlanDay day) {
        List<WorkoutPlanExerciseResponse> exRes = Optional.ofNullable(day.getExercises())
                .orElse(Collections.emptyList()).stream()
                .map(this::buildExerciseResponse).collect(Collectors.toList());
        return WorkoutPlanDayResponse.builder()
                .id(day.getId()).dayOfWeek(day.getDayOfWeek()).dayName(day.getDayName())
                .exercises(exRes).build();
    }

    private WorkoutPlanExerciseResponse buildExerciseResponse(WorkoutPlanExercise pe) {
        return WorkoutPlanExerciseResponse.builder()
                .id(pe.getId()).exerciseId(pe.getExercise().getId())
                .exerciseName(pe.getExercise().getName())
                .muscleGroup(pe.getExercise().getMuscleGroup() != null ? pe.getExercise().getMuscleGroup().name() : null)
                .difficulty(pe.getExercise().getDifficulty()  != null ? pe.getExercise().getDifficulty().name()  : null)
                .sets(pe.getSets()).reps(pe.getReps()).durationSeconds(pe.getDurationSeconds())
                .restSeconds(pe.getRestSeconds()).orderIndex(pe.getOrderIndex())
                .notes(pe.getNotes()).videoUrl(pe.getExercise().getVideoUrl())
                .caloriesBurned(pe.getExercise().getCaloriesBurned()).build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}