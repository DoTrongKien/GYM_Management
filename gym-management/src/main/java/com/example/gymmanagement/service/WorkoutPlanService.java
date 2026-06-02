package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.WorkoutPlanRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.*;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutPlanService {

    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanDayRepository planDayRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserProfileRepository profileRepository;

    // ── Tạo giáo án AI theo hồ sơ ───────────────────────────
    @Transactional
    public WorkoutPlanResponse generateAIPlan(String email) {
        User user = getUser(email);
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Hãy hoàn thiện hồ sơ trước khi tạo giáo án."));

        Goal goal       = profile.getGoal()          != null ? profile.getGoal()          : Goal.MAINTENANCE;
        FitnessLevel lv = profile.getFitnessLevel()   != null ? profile.getFitnessLevel()   : FitnessLevel.BEGINNER;
        int days        = profile.getAvailableDaysPerWeek() != null ? profile.getAvailableDaysPerWeek() : 3;

        return buildAndSavePlan(user, goal, lv, days, true);
    }

    // ── Tạo giáo án AI với mục tiêu tuỳ chọn ───────────────
    @Transactional
    public WorkoutPlanResponse generateAIPlanWithGoal(String email, Goal goal,
                                                      FitnessLevel level, Integer daysPerWeek) {
        User user = getUser(email);

        if (daysPerWeek == null || daysPerWeek < 1) {
            daysPerWeek = profileRepository.findByUserId(user.getId())
                    .map(p -> p.getAvailableDaysPerWeek() != null ? p.getAvailableDaysPerWeek() : 3).orElse(3);
        }
        if (level == null) {
            level = profileRepository.findByUserId(user.getId())
                    .map(p -> p.getFitnessLevel() != null ? p.getFitnessLevel() : FitnessLevel.BEGINNER)
                    .orElse(FitnessLevel.BEGINNER);
        }
        return buildAndSavePlan(user, goal, level, daysPerWeek, true);
    }

    private WorkoutPlanResponse buildAndSavePlan(User user, Goal goal, FitnessLevel level,
                                                 int daysPerWeek, boolean isAi) {
        // Deactivate plan cũ
        planRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(p -> {
            p.setIsActive(false);
            planRepository.save(p);
        });

        WorkoutPlan plan = WorkoutPlan.builder()
                .user(user)
                .planName(generatePlanName(goal, level))
                .description(generatePlanDescription(goal, level, daysPerWeek))
                .goal(goal).targetLevel(level)
                .durationWeeks(8).sessionsPerWeek(daysPerWeek)
                .isActive(true).isAiGenerated(isAi)
                .build();
        planRepository.save(plan);

        // Tạo ngày mẫu (template only - KHÔNG tạo sessions)
        List<WorkoutPlanDay> days = generatePlanDays(plan, goal, level, daysPerWeek);
        planDayRepository.saveAll(days);

        plan.setPlanDays(days);
        return buildPlanResponse(plan);
    }

    // ── Tạo giáo án thủ công ────────────────────────────────
    @Transactional
    public WorkoutPlanResponse createCustomPlan(String email, WorkoutPlanRequest request) {
        User user = getUser(email);
        planRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(p -> {
            p.setIsActive(false); planRepository.save(p);
        });
        WorkoutPlan plan = WorkoutPlan.builder()
                .user(user).planName(request.getPlanName()).description(request.getDescription())
                .goal(request.getGoal()).targetLevel(request.getTargetLevel())
                .durationWeeks(request.getDurationWeeks() != null ? request.getDurationWeeks() : 4)
                .sessionsPerWeek(request.getSessionsPerWeek() != null ? request.getSessionsPerWeek() : 3)
                .isActive(true).isAiGenerated(false).build();
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
        return planRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(p -> {
            p.setPlanDays(planDayRepository.findByWorkoutPlanIdOrderByDayOfWeek(p.getId()));
            return buildPlanResponse(p);
        }).collect(Collectors.toList());
    }

    // ── Tạo ngày mẫu ────────────────────────────────────────
    private List<WorkoutPlanDay> generatePlanDays(WorkoutPlan plan, Goal goal,
                                                  FitnessLevel level, int daysPerWeek) {
        List<List<MuscleGroup>> configs = getDayConfigs(goal, daysPerWeek);
        int[] dowMap  = {1, 3, 5, 2, 4, 6, 7};
        String[] names = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
        List<WorkoutPlanDay> days = new ArrayList<>();

        for (int i = 0; i < Math.min(daysPerWeek, configs.size()); i++) {
            WorkoutPlanDay day = WorkoutPlanDay.builder()
                    .workoutPlan(plan).dayOfWeek(dowMap[i]).dayName(names[dowMap[i] - 1]).build();
            day.setExercises(generateExercisesForDay(day, configs.get(i), goal, level));
            days.add(day);
        }
        return days;
    }

    private List<List<MuscleGroup>> getDayConfigs(Goal goal, int days) {
        List<List<MuscleGroup>> all = switch (goal) {
            case MUSCLE_GAIN -> List.of(
                    List.of(MuscleGroup.CHEST, MuscleGroup.ARMS),
                    List.of(MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                    List.of(MuscleGroup.LEGS),
                    List.of(MuscleGroup.CHEST, MuscleGroup.CORE),
                    List.of(MuscleGroup.BACK, MuscleGroup.ARMS),
                    List.of(MuscleGroup.LEGS, MuscleGroup.SHOULDERS),
                    List.of(MuscleGroup.FULL_BODY)
            );
            case WEIGHT_LOSS -> List.of(
                    List.of(MuscleGroup.CARDIO, MuscleGroup.CORE),
                    List.of(MuscleGroup.FULL_BODY),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.LEGS),
                    List.of(MuscleGroup.BACK, MuscleGroup.CHEST),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.ARMS),
                    List.of(MuscleGroup.FULL_BODY, MuscleGroup.CORE),
                    List.of(MuscleGroup.CARDIO)
            );
            case ENDURANCE -> List.of(
                    List.of(MuscleGroup.CARDIO),
                    List.of(MuscleGroup.FULL_BODY),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.CORE),
                    List.of(MuscleGroup.LEGS, MuscleGroup.CARDIO),
                    List.of(MuscleGroup.FULL_BODY, MuscleGroup.CORE),
                    List.of(MuscleGroup.CARDIO),
                    List.of(MuscleGroup.FULL_BODY)
            );
            case FLEXIBILITY -> List.of(
                    List.of(MuscleGroup.FULL_BODY, MuscleGroup.CORE),
                    List.of(MuscleGroup.LEGS, MuscleGroup.BACK),
                    List.of(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                    List.of(MuscleGroup.FULL_BODY),
                    List.of(MuscleGroup.CORE, MuscleGroup.LEGS),
                    List.of(MuscleGroup.BACK, MuscleGroup.CHEST),
                    List.of(MuscleGroup.FULL_BODY)
            );
            default -> List.of(
                    List.of(MuscleGroup.FULL_BODY),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.CORE),
                    List.of(MuscleGroup.CHEST, MuscleGroup.BACK),
                    List.of(MuscleGroup.LEGS),
                    List.of(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                    List.of(MuscleGroup.CARDIO, MuscleGroup.FULL_BODY),
                    List.of(MuscleGroup.CORE)
            );
        };
        return all.subList(0, Math.min(days, all.size()));
    }

    private List<WorkoutPlanExercise> generateExercisesForDay(WorkoutPlanDay day,
                                                              List<MuscleGroup> groups, Goal goal, FitnessLevel level) {
        List<WorkoutPlanExercise> result = new ArrayList<>();
        int perGroup = level == FitnessLevel.BEGINNER ? 2 : level == FitnessLevel.INTERMEDIATE ? 3 : 4;
        int idx = 1;

        for (MuscleGroup mg : groups) {
            List<Exercise> candidates = getByGoalScore(mg, goal);
            int take = Math.min(candidates.size(), perGroup);
            for (int i = 0; i < take; i++) {
                Exercise ex = candidates.get(i);
                int[] sr = adjustSetsReps(ex, goal, level);
                result.add(WorkoutPlanExercise.builder()
                        .planDay(day).exercise(ex)
                        .sets(sr[0])
                        .reps(ex.getDefaultReps() != null ? sr[1] : null)
                        .durationSeconds(ex.getDefaultDurationSeconds() != null
                                ? adjustDuration(ex.getDefaultDurationSeconds(), level) : null)
                        .restSeconds(adjustRest(ex.getRestSeconds(), goal))
                        .orderIndex(idx++).notes(buildNote(ex, goal)).build());
            }
        }
        return result;
    }

    private List<Exercise> getByGoalScore(MuscleGroup mg, Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN -> exerciseRepository.findByMuscleGroupOrderByMuscleGain(mg);
            case WEIGHT_LOSS -> exerciseRepository.findByMuscleGroupOrderByWeightLoss(mg);
            case ENDURANCE   -> exerciseRepository.findByMuscleGroupOrderByEndurance(mg);
            case FLEXIBILITY -> exerciseRepository.findByMuscleGroupOrderByFlexibility(mg);
            default          -> exerciseRepository.findByMuscleGroupOrderByMaintenance(mg);
        };
    }

    private int[] adjustSetsReps(Exercise ex, Goal goal, FitnessLevel lv) {
        int sets, reps = ex.getDefaultReps() != null ? ex.getDefaultReps() : 12;
        sets = switch (goal) {
            case MUSCLE_GAIN -> lv == FitnessLevel.BEGINNER ? 3 : lv == FitnessLevel.INTERMEDIATE ? 4 : 5;
            case WEIGHT_LOSS -> 3;
            case ENDURANCE   -> lv == FitnessLevel.BEGINNER ? 2 : 3;
            default          -> ex.getDefaultSets() != null ? ex.getDefaultSets() : 3;
        };
        reps = switch (goal) {
            case MUSCLE_GAIN -> lv == FitnessLevel.BEGINNER ? 10 : lv == FitnessLevel.INTERMEDIATE ? 8 : 6;
            case WEIGHT_LOSS -> lv == FitnessLevel.BEGINNER ? 15 : 20;
            case ENDURANCE   -> lv == FitnessLevel.BEGINNER ? 15 : 25;
            default          -> reps;
        };
        return new int[]{sets, reps};
    }

    private int adjustDuration(int base, FitnessLevel lv) {
        return switch (lv) { case BEGINNER -> (int)(base*0.7); case ADVANCED -> (int)(base*1.3); default -> base; };
    }

    private int adjustRest(Integer base, Goal goal) {
        if (base == null) base = 60;
        return switch (goal) {
            case MUSCLE_GAIN -> (int)(base*1.3);
            case WEIGHT_LOSS -> (int)(base*0.7);
            case ENDURANCE   -> (int)(base*0.6);
            case FLEXIBILITY -> 30;
            default          -> base;
        };
    }

    private String buildNote(Exercise ex, Goal goal) {
        int score = switch (goal) {
            case MUSCLE_GAIN -> ex.getMuscleGainScore()  != null ? ex.getMuscleGainScore()  : 0;
            case WEIGHT_LOSS -> ex.getWeightLossScore()  != null ? ex.getWeightLossScore()  : 0;
            case ENDURANCE   -> ex.getEnduranceScore()   != null ? ex.getEnduranceScore()   : 0;
            case FLEXIBILITY -> ex.getFlexibilityScore() != null ? ex.getFlexibilityScore() : 0;
            default          -> ex.getMaintenanceScore() != null ? ex.getMaintenanceScore() : 0;
        };
        if (score >= 9) return "⭐ Hàng đầu cho mục tiêu này";
        if (score >= 7) return "✅ Phù hợp tốt với mục tiêu";
        return null;
    }

    private String generatePlanName(Goal goal, FitnessLevel lv) {
        String g = switch (goal) {
            case WEIGHT_LOSS -> "Fat Burning"; case MUSCLE_GAIN -> "Muscle Building";
            case ENDURANCE   -> "Endurance";   case FLEXIBILITY -> "Flexibility";
            default          -> "Balanced";
        };
        String l = switch (lv) { case BEGINNER -> "Starter"; case ADVANCED -> "Elite"; default -> "Progress"; };
        return g + " " + l + " Plan";
    }

    private String generatePlanDescription(Goal goal, FitnessLevel lv, int days) {
        String gv = switch (goal) {
            case WEIGHT_LOSS -> "giảm cân & đốt mỡ"; case MUSCLE_GAIN -> "tăng cơ & sức mạnh";
            case ENDURANCE   -> "tăng sức bền";       case FLEXIBILITY -> "tăng linh hoạt";
            default          -> "duy trì thể hình";
        };
        return String.format("Giáo án AI tối ưu cho mục tiêu %s, trình độ %s. %d ngày mẫu/tuần, 8 tuần. " +
                "Đăng ký từng buổi theo lịch của bạn.", gv, lv.name().toLowerCase(), days);
    }

    // ── Response builders ────────────────────────────────────
    public WorkoutPlanResponse buildPlanResponse(WorkoutPlan plan) {
        return WorkoutPlanResponse.builder()
                .id(plan.getId()).planName(plan.getPlanName()).description(plan.getDescription())
                .goal(plan.getGoal()).targetLevel(plan.getTargetLevel())
                .durationWeeks(plan.getDurationWeeks()).sessionsPerWeek(plan.getSessionsPerWeek())
                .isActive(plan.getIsActive()).isAiGenerated(plan.getIsAiGenerated())
                .createdAt(plan.getCreatedAt())
                .planDays(Optional.ofNullable(plan.getPlanDays()).orElse(Collections.emptyList())
                        .stream().map(this::buildDayResponse).collect(Collectors.toList()))
                .build();
    }

    private WorkoutPlanDayResponse buildDayResponse(WorkoutPlanDay day) {
        return WorkoutPlanDayResponse.builder()
                .id(day.getId()).dayOfWeek(day.getDayOfWeek()).dayName(day.getDayName())
                .exercises(Optional.ofNullable(day.getExercises()).orElse(Collections.emptyList())
                        .stream().map(this::buildExResponse).collect(Collectors.toList()))
                .build();
    }

    private WorkoutPlanExerciseResponse buildExResponse(WorkoutPlanExercise pe) {
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