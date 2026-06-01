package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.GeneratePlanWithGoalRequest;
import com.example.gymmanagement.dto.request.WorkoutPlanRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import com.example.gymmanagement.service.WorkoutPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workout-plans")
@RequiredArgsConstructor
public class WorkoutPlanController {

    private final WorkoutPlanService planService;

    // Tạo giáo án AI dựa hoàn toàn theo hồ sơ
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> generateAIPlan(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.generateAIPlan(ud.getUsername()),
                "Giáo án AI đã được tạo theo hồ sơ của bạn!"));
    }

    // Tạo giáo án AI với mục tiêu tuỳ chọn
    @PostMapping("/generate-with-goal")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> generateWithGoal(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody GeneratePlanWithGoalRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.generateAIPlanWithGoal(
                        ud.getUsername(),
                        request.getGoal(),
                        request.getFitnessLevel(),
                        request.getDaysPerWeek()
                ),
                "Giáo án đã được tạo theo mục tiêu: " + request.getGoal()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> createCustomPlan(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody WorkoutPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.createCustomPlan(ud.getUsername(), request),
                "Giáo án đã được tạo!"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> getActivePlan(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(planService.getActivePlan(ud.getUsername())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutPlanResponse>>> getAllPlans(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(planService.getAllPlans(ud.getUsername())));
    }

    // Danh sách Goals để FE hiển thị cho user chọn
    @GetMapping("/goals")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, String>>>> getGoals() {
        List<java.util.Map<String, String>> goals = List.of(
                java.util.Map.of("value","MUSCLE_GAIN",  "label","💪 Tăng cơ / Tăng sức mạnh"),
                java.util.Map.of("value","WEIGHT_LOSS",  "label","🔥 Giảm cân / Đốt mỡ"),
                java.util.Map.of("value","ENDURANCE",    "label","🏃 Tăng sức bền"),
                java.util.Map.of("value","FLEXIBILITY",  "label","🤸 Tăng độ linh hoạt"),
                java.util.Map.of("value","MAINTENANCE",  "label","⚖️ Duy trì thể hình")
        );
        return ResponseEntity.ok(ApiResponse.success(goals));
    }
}