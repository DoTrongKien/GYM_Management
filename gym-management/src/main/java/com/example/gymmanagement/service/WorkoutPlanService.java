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

    @Transactional
    public WorkoutPlanResponse generateAIPlan(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Please complete your profile first before generating a plan."));

        // Deactivate old plan
        planRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(p -> {
            p.setIsActive(false);
            planRepository.save(p);
        });

        Goal goal = profile.getGoal() != null ? profile.getGoal() : Goal.MAINTENANCE;
        FitnessLevel level = profile.getFitnessLevel() != null ? profile.getFitnessLevel() : FitnessLevel.BEGINNER;
        int daysPerWeek = profile.getAvailableDaysPerWeek() != null ? profile.getAvailableDaysPerWeek() : 3;

        WorkoutPlan plan = WorkoutPlan.builder()
                .user(user)
                .planName(generatePlanName(goal, level))
                .description(generatePlanDescription(goal, level, daysPerWeek))
                .goal(goal)
                .targetLevel(level)
                .durationWeeks(8)
                .sessionsPerWeek(daysPerWeek)
                .isActive(true)
                .isAiGenerated(true)
                .build();
        planRepository.save(plan);

        // Generate plan days
        List<WorkoutPlanDay> days = generatePlanDays(plan, goal, level, daysPerWeek);
        planDayRepository.saveAll(days);

        // Generate workout sessions for 8 weeks
        generateWeeklySessions(user, plan, days, 8);

        plan.setPlanDays(days);
        return buildPlanResponse(plan);
    }

    @Transactional
    public WorkoutPlanResponse createCustomPlan(String email, WorkoutPlanRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Deactivate old active plan
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WorkoutPlan plan = planRepository.findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new RuntimeException("No active workout plan. Please generate a plan first."));

        plan.setPlanDays(planDayRepository.findByWorkoutPlanIdOrderByDayOfWeek(plan.getId()));
        return buildPlanResponse(plan);
    }

    public List<WorkoutPlanResponse> getAllPlans(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return planRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(p -> {
                    p.setPlanDays(planDayRepository.findByWorkoutPlanIdOrderByDayOfWeek(p.getId()));
                    return buildPlanResponse(p);
                })
                .collect(Collectors.toList());
    }

    private List<WorkoutPlanDay> generatePlanDays(WorkoutPlan plan, Goal goal, FitnessLevel level, int daysPerWeek) {
        // Day configurations based on goal
        List<String> dayConfigs = getDayConfigs(goal, daysPerWeek);
        List<WorkoutPlanDay> days = new ArrayList<>();

        int[] dayOfWeekMap = {1, 3, 5, 2, 4, 6, 7}; // Mon, Wed, Fri, Tue, Thu, Sat, Sun
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (int i = 0; i < daysPerWeek && i < dayConfigs.size(); i++) {
            WorkoutPlanDay day = WorkoutPlanDay.builder()
                    .workoutPlan(plan)
                    .dayOfWeek(dayOfWeekMap[i])
                    .dayName(dayNames[dayOfWeekMap[i] - 1])
                    .build();

            String muscleGroupStr = dayConfigs.get(i);
            List<WorkoutPlanExercise> exercises = generateExercisesForDay(day, muscleGroupStr, level);
            day.setExercises(exercises);
            days.add(day);
        }
        return days;
    }

    private List<String> getDayConfigs(Goal goal, int daysPerWeek) {
        if (goal == Goal.WEIGHT_LOSS) {
            return List.of("CARDIO,CORE", "FULL_BODY", "CARDIO,LEGS", "BACK,CHEST", "CARDIO,ARMS", "FULL_BODY", "CARDIO").subList(0, daysPerWeek);
        } else if (goal == Goal.MUSCLE_GAIN) {
            return List.of("CHEST,ARMS", "BACK,SHOULDERS", "LEGS", "CHEST,CORE", "BACK,ARMS", "LEGS,SHOULDERS", "FULL_BODY").subList(0, daysPerWeek);
        } else {
            return List.of("FULL_BODY", "CARDIO,CORE", "FULL_BODY", "LEGS", "CHEST,BACK", "CARDIO", "CORE").subList(0, daysPerWeek);
        }
    }

    private List<WorkoutPlanExercise> generateExercisesForDay(WorkoutPlanDay day, String muscleGroupStr, FitnessLevel level) {
        List<WorkoutPlanExercise> exercises = new ArrayList<>();
        String[] groups = muscleGroupStr.split(",");
        int orderIndex = 1;

        Difficulty difficulty = level == FitnessLevel.BEGINNER ? Difficulty.EASY :
                level == FitnessLevel.INTERMEDIATE ? Difficulty.MEDIUM : Difficulty.HARD;

        for (String group : groups) {
            try {
                MuscleGroup mg = MuscleGroup.valueOf(group.trim());
                List<Exercise> available = exerciseRepository.findByMuscleGroupAndIsActiveTrue(mg);
                int count = Math.min(available.size(), level == FitnessLevel.BEGINNER ? 2 : 3);
                for (int i = 0; i < count; i++) {
                    Exercise ex = available.get(i);
                    WorkoutPlanExercise planEx = WorkoutPlanExercise.builder()
                            .planDay(day)
                            .exercise(ex)
                            .sets(ex.getDefaultSets() != null ? ex.getDefaultSets() : 3)
                            .reps(ex.getDefaultReps())
                            .durationSeconds(ex.getDefaultDurationSeconds())
                            .restSeconds(ex.getRestSeconds() != null ? ex.getRestSeconds() : 60)
                            .orderIndex(orderIndex++)
                            .build();
                    exercises.add(planEx);
                }
            } catch (IllegalArgumentException e) {
                // skip unknown muscle group
            }
        }
        return exercises;
    }

    private void generateWeeklySessions(User user, WorkoutPlan plan, List<WorkoutPlanDay> days, int weeks) {
        List<WorkoutSession> sessions = new ArrayList<>();
        LocalDate startDate = LocalDate.now();

        for (int week = 1; week <= weeks; week++) {
            for (WorkoutPlanDay day : days) {
                LocalDate sessionDate = startDate.plusWeeks(week - 1).with(
                        java.time.DayOfWeek.of(day.getDayOfWeek()));

                WorkoutSession session = WorkoutSession.builder()
                        .user(user)
                        .workoutPlan(plan)
                        .planDay(day)
                        .sessionDate(sessionDate)
                        .status(SessionStatus.SCHEDULED)
                        .weekNumber(week)
                        .build();
                sessions.add(session);
            }
        }
        sessionRepository.saveAll(sessions);
    }

    private String generatePlanName(Goal goal, FitnessLevel level) {
        String goalName = switch (goal) {
            case WEIGHT_LOSS -> "Fat Burning";
            case MUSCLE_GAIN -> "Muscle Building";
            case ENDURANCE -> "Endurance";
            case FLEXIBILITY -> "Flexibility";
            default -> "Balanced Fitness";
        };
        String levelName = switch (level) {
            case BEGINNER -> "Starter";
            case INTERMEDIATE -> "Progress";
            case ADVANCED -> "Elite";
        };
        return goalName + " " + levelName + " Plan";
    }

    private String generatePlanDescription(Goal goal, FitnessLevel level, int daysPerWeek) {
        return String.format("AI-generated %s workout plan for %s level. %d sessions per week over 8 weeks. " +
                        "Designed to help you achieve your %s goal effectively.",
                level.name().toLowerCase(), level.name().toLowerCase(), daysPerWeek, goal.name().toLowerCase().replace("_", " "));
    }

    public WorkoutPlanResponse buildPlanResponse(WorkoutPlan plan) {
        List<WorkoutPlanDayResponse> dayResponses = Optional.ofNullable(plan.getPlanDays())
                .orElse(Collections.emptyList()).stream()
                .map(this::buildDayResponse)
                .collect(Collectors.toList());

        return WorkoutPlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getPlanName())
                .description(plan.getDescription())
                .goal(plan.getGoal())
                .targetLevel(plan.getTargetLevel())
                .durationWeeks(plan.getDurationWeeks())
                .sessionsPerWeek(plan.getSessionsPerWeek())
                .isActive(plan.getIsActive())
                .isAiGenerated(plan.getIsAiGenerated())
                .createdAt(plan.getCreatedAt())
                .planDays(dayResponses)
                .build();
    }

    private WorkoutPlanDayResponse buildDayResponse(WorkoutPlanDay day) {
        List<WorkoutPlanExerciseResponse> exerciseResponses = Optional.ofNullable(day.getExercises())
                .orElse(Collections.emptyList()).stream()
                .map(this::buildExerciseResponse)
                .collect(Collectors.toList());

        return WorkoutPlanDayResponse.builder()
                .id(day.getId())
                .dayOfWeek(day.getDayOfWeek())
                .dayName(day.getDayName())
                .exercises(exerciseResponses)
                .build();
    }

    private WorkoutPlanExerciseResponse buildExerciseResponse(WorkoutPlanExercise pe) {
        return WorkoutPlanExerciseResponse.builder()
                .id(pe.getId())
                .exerciseId(pe.getExercise().getId())
                .exerciseName(pe.getExercise().getName())
                .muscleGroup(pe.getExercise().getMuscleGroup() != null ? pe.getExercise().getMuscleGroup().name() : null)
                .difficulty(pe.getExercise().getDifficulty() != null ? pe.getExercise().getDifficulty().name() : null)
                .sets(pe.getSets())
                .reps(pe.getReps())
                .durationSeconds(pe.getDurationSeconds())
                .restSeconds(pe.getRestSeconds())
                .orderIndex(pe.getOrderIndex())
                .notes(pe.getNotes())
                .videoUrl(pe.getExercise().getVideoUrl())
                .caloriesBurned(pe.getExercise().getCaloriesBurned())
                .build();
    }
}