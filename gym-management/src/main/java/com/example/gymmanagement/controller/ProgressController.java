package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.ProgressRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProgressResponse>> addProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ProgressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                progressService.addProgress(userDetails.getUsername(), request),
                "Progress recorded successfully!"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getMyProgress(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(progressService.getMyProgress(userDetails.getUsername())));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<ProgressResponse>> getLatest(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(progressService.getLatestProgress(userDetails.getUsername())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgressResponse>> updateProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody ProgressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                progressService.updateProgress(userDetails.getUsername(), id, request)));
    }
}