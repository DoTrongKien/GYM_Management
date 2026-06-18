package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.WorkoutPlanRequest;
import com.example.gymmanagement.dto.response.WorkoutPlanResponse;
import com.example.gymmanagement.entity.WorkoutPlan;
import com.example.gymmanagement.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminWorkoutPlanService {
    private final WorkoutPlanRepository workoutPlanRepository;

    public List<WorkoutPlanResponse> getAllPlans(){
        return workoutPlanRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WorkoutPlanResponse getPlanById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public WorkoutPlanResponse createPlan(WorkoutPlanRequest request) {

        WorkoutPlan plan = new WorkoutPlan();

        plan.setPlanName(request.getPlanName());
        plan.setDescription(request.getDescription());
        plan.setGoal(request.getGoal());
        plan.setTargetLevel(request.getTargetLevel());
        plan.setDurationWeeks(request.getDurationWeeks());
        plan.setSessionsPerWeek(request.getSessionsPerWeek());
        plan.setIsActive(true);

        return toResponse(workoutPlanRepository.save(plan));
    }

    public WorkoutPlanResponse updatePlan(
            Long id,
            WorkoutPlanRequest request) {

        WorkoutPlan plan = findOrThrow(id);

        plan.setPlanName(request.getPlanName());
        plan.setDescription(request.getDescription());
        plan.setGoal(request.getGoal());
        plan.setTargetLevel(request.getTargetLevel());
        plan.setDurationWeeks(request.getDurationWeeks());
        plan.setSessionsPerWeek(request.getSessionsPerWeek());

        return toResponse(workoutPlanRepository.save(plan));
    }

    public void deletePlan(Long id) {

        WorkoutPlan plan = findOrThrow(id);

        plan.setIsActive(false);

        workoutPlanRepository.save(plan);
    }

    private WorkoutPlan findOrThrow(Long id) {
        return workoutPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workout plan not found"));
    }

    // Map entity -> DTO thủ công, KHÔNG đụng tới field "user" hay "planDays"
    // (đều là quan hệ lazy do Hibernate proxy) để tránh lỗi
    // "Type definition error: ... ByteBuddyInterceptor" khi Jackson serialize.
    private WorkoutPlanResponse toResponse(WorkoutPlan plan) {
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
                .isCompleted(plan.getIsCompleted())
                .weekStartDate(plan.getWeekStartDate())
                .createdAt(plan.getCreatedAt())
                .startingBmi(plan.getStartingBmi())
                .startingWeight(plan.getStartingWeight())
                .difficultyAdjustment(plan.getDifficultyAdjustment())
                .setsAdjustment(plan.getSetsAdjustment())
                .repsAdjustment(plan.getRepsAdjustment())
                .build();
    }
}