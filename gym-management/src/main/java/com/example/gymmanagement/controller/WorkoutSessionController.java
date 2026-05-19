package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.CheckInRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.service.WorkoutSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class WorkoutSessionController {

    private final WorkoutSessionService sessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutSessionResponse>>> getMySessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getMySessions(userDetails.getUsername())));
    }

    @GetMapping("/this-week")
    public ResponseEntity<ApiResponse<List<WorkoutSessionResponse>>> getWeekSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getWeekSessions(userDetails.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> getSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getSessionById(userDetails.getUsername(), id)));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> checkIn(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.checkIn(userDetails.getUsername(), id), "Check-in successful! Let's go!"));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> completeSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.completeSession(userDetails.getUsername(), id, request),
                "Session completed! Great job!"));
    }

    @PostMapping("/{id}/skip")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> skipSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.skipSession(userDetails.getUsername(), id, notes)));
    }
}