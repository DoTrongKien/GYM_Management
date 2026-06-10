package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.*;
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

    @PostMapping("/enroll")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> enroll(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody EnrollSessionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.enrollSession(ud.getUsername(), req), "Đã đăng ký lịch tập!"));
    }

    @GetMapping("/week-progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> weekProgress(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam Long planId, @RequestParam Integer weekNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.getWeekProgress(ud.getUsername(), planId, weekNumber)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutSessionResponse>>> getAll(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getMySessions(ud.getUsername())));
    }

    @GetMapping("/this-week")
    public ResponseEntity<ApiResponse<List<WorkoutSessionResponse>>> getWeek(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getWeekSessions(ud.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> getOne(
            @AuthenticationPrincipal UserDetails ud, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getSessionById(ud.getUsername(), id)));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> checkIn(
            @AuthenticationPrincipal UserDetails ud, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.checkIn(ud.getUsername(), id), "Check-in thành công! 💪"));
    }

    // Check-out bắt buộc nhập tỉ lệ hoàn thành + tiến độ (cuối tuần)
    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> checkOut(
            @AuthenticationPrincipal UserDetails ud, @PathVariable Long id,
            @RequestBody CheckOutRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.checkOut(ud.getUsername(), id, req), "Check-out thành công! 🎉"));
    }

    @PostMapping("/{id}/skip")
    public ResponseEntity<ApiResponse<WorkoutSessionResponse>> skip(
            @AuthenticationPrincipal UserDetails ud, @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.skipSession(ud.getUsername(), id,
                        body != null ? body.get("notes") : null)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails ud, @PathVariable Long id) {
        sessionService.deleteSession(ud.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa lịch tập"));
    }
}