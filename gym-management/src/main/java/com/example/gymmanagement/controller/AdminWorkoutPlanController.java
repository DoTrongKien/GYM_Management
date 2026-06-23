package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.WorkoutPlanRequest;
import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.dto.response.WorkoutPlanResponse;
import com.example.gymmanagement.service.AdminWorkoutPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/workout-plans")
@RequiredArgsConstructor
public class AdminWorkoutPlanController {
    private final AdminWorkoutPlanService adminWorkoutPlanService;
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<WorkoutPlanResponse>>> getALLPlans(){
        return ResponseEntity.ok(
                ApiResponse.success(
                        adminWorkoutPlanService.getAllPlans()
                )
        );
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> getPlanById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        adminWorkoutPlanService.getPlanById(id)
                )
        );
    }
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> createPlan(
            @RequestBody WorkoutPlanRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        adminWorkoutPlanService.createPlan(request),
                        "Plan created successfully"
                )
        );
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> updatePlan(
            @PathVariable Long id,
            @RequestBody WorkoutPlanRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        adminWorkoutPlanService.updatePlan(id, request),
                        "Plan updated successfully"
                )
        );

    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @PathVariable Long id) {

        adminWorkoutPlanService.deletePlan(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Plan deleted successfully"
                )
        );
    }
}