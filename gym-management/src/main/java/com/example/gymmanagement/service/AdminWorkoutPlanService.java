package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.WorkoutTemplateRequest;
import com.example.gymmanagement.dto.response.WorkoutPlanResponse;
import com.example.gymmanagement.entity.WorkoutPlan;
import com.example.gymmanagement.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminWorkoutPlanService {
    private final WorkoutPlanRepository workoutPlanRepository;
    // Gọi sang đây để tái dùng logic build planDays + response đầy đủ
    private final WorkoutPlanService workoutPlanService;

    // ─── List / Get (mọi plan, gồm cả template + plan của user) ───
    public List<WorkoutPlanResponse> getAllPlans() {
        return workoutPlanRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WorkoutPlanResponse getPlanById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public void deletePlan(Long id) {
        WorkoutPlan plan = findOrThrow(id);
        plan.setIsActive(false);
        workoutPlanRepository.save(plan);
    }

    // ─── Template (giáo án thủ công do admin tạo) ──────────────
    // Toàn bộ logic tạo/sửa/copy thực tế nằm ở WorkoutPlanService
    // để tái dùng buildDaysFromRequest + toPlanResponse (có kèm planDays đầy đủ).

    public WorkoutPlanResponse createTemplate(WorkoutTemplateRequest req) {
        return workoutPlanService.createManualTemplate(req);
    }

    public WorkoutPlanResponse updateTemplate(Long id, WorkoutTemplateRequest req) {
        return workoutPlanService.updateManualTemplate(id, req);
    }

    public List<WorkoutPlanResponse> getAllTemplates() {
        return workoutPlanService.getAllTemplates(true);
    }

    public void deleteTemplate(Long id) {
        workoutPlanService.deleteTemplate(id);
    }

    // ─── Helpers ─────────────────────────────────────────────
    private WorkoutPlan findOrThrow(Long id) {
        return workoutPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workout plan not found"));
    }

    // Map entity -> DTO thủ công, KHÔNG đụng tới field "user" hay "planDays"
    // (đều là quan hệ lazy do Hibernate proxy) để tránh lỗi
    // "Type definition error: ... ByteBuddyInterceptor" khi Jackson serialize.
    // Dùng cho list tổng quát (không cần planDays chi tiết).
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
                .isTemplate(plan.getIsTemplate())
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