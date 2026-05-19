package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.service.NutritionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutrition")
@RequiredArgsConstructor
public class NutritionController {

    private final NutritionService nutritionService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<NutritionResponse>> generate(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                nutritionService.generateNutritionPlan(userDetails.getUsername()),
                "Nutrition plan generated based on your profile!"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NutritionResponse>> getLatest(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(nutritionService.getLatestPlan(userDetails.getUsername())));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<NutritionResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(nutritionService.getAllPlans(userDetails.getUsername())));
    }
}