package com.example.gymmanagement.controller;

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

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> generateAIPlan(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.generateAIPlan(userDetails.getUsername()),
                "AI workout plan generated successfully!"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> createCustomPlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody WorkoutPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.createCustomPlan(userDetails.getUsername(), request),
                "Workout plan created"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> getActivePlan(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(planService.getActivePlan(userDetails.getUsername())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutPlanResponse>>> getAllPlans(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(planService.getAllPlans(userDetails.getUsername())));
    }
}