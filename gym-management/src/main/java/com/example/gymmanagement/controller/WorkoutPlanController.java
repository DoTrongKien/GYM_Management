package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.GeneratePlanRequest;
import com.example.gymmanagement.dto.request.WorkoutPlanRequest;
import com.example.gymmanagement.dto.response.*;
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

    // Tạo giáo án AI từ profile (không cần body)
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> generateFromProfile(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.generateAIPlan(ud.getUsername()),
                "Giáo án AI đã được tạo thành công!"));
    }

    // Tạo giáo án AI với mục tiêu tùy chọn (có thể override profile)
    @PostMapping("/generate-custom")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> generateWithGoal(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody GeneratePlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.generatePlanWithGoal(
                        ud.getUsername(),
                        request.getGoal(),
                        request.getFitnessLevel(),
                        request.getAvailableDaysPerWeek()
                ),
                "Giáo án đã được tạo theo mục tiêu của bạn!"));
    }

    // Tạo giáo án thủ công
    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> createCustom(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody WorkoutPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.createCustomPlan(ud.getUsername(), request),
               "Giáo án đã được tạo!"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> getActive(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(planService.getActivePlan(ud.getUsername())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutPlanResponse>>> getAll(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(planService.getAllPlans(ud.getUsername())));
    }
}